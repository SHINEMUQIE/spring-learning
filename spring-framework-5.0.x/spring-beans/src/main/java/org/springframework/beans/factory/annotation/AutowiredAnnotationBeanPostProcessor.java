/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.annotation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.LookupOverride;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that autowires annotated fields, setter methods and arbitrary config methods.
 * Such members to be injected are detected through a Java 5 annotation: by default,
 * Spring's {@link Autowired @Autowired} and {@link Value @Value} annotations.
 *
 * <p>Also supports JSR-330's {@link javax.inject.Inject @Inject} annotation,
 * if available, as a direct alternative to Spring's own {@code @Autowired}.
 *
 * <p>Only one constructor (at max) of any given bean class may declare this annotation
 * with the 'required' parameter set to {@code true}, indicating <i>the</i> constructor
 * to autowire when used as a Spring bean. If multiple <i>non-required</i> constructors
 * declare the annotation, they will be considered as candidates for autowiring.
 * The constructor with the greatest number of dependencies that can be satisfied by
 * matching beans in the Spring container will be chosen. If none of the candidates
 * can be satisfied, then a primary/default constructor (if present) will be used.
 * If a class only declares a single constructor to begin with, it will always be used,
 * even if not annotated. An annotated constructor does not have to be public.
 *
 * <p>Fields are injected right after construction of a bean, before any
 * config methods are invoked. Such a config field does not have to be public.
 *
 * <p>Config methods may have an arbitrary name and any number of arguments; each of
 * those arguments will be autowired with a matching bean in the Spring container.
 * Bean property setter methods are effectively just a special case of such a
 * general config method. Config methods do not have to be public.
 *
 * <p>Note: A default AutowiredAnnotationBeanPostProcessor will be registered
 * by the "context:annotation-config" and "context:component-scan" XML tags.
 * Remove or turn off the default annotation configuration there if you intend
 * to specify a custom AutowiredAnnotationBeanPostProcessor bean definition.
 * <p><b>NOTE:</b> Annotation injection will be performed <i>before</i> XML injection;
 * thus the latter configuration will override the former for properties wired through
 * both approaches.
 *
 * <p>In addition to regular injection points as discussed above, this post-processor
 * also handles Spring's {@link Lookup @Lookup} annotation which identifies lookup
 * methods to be replaced by the container at runtime. This is essentially a type-safe
 * version of {@code getBean(Class, args)} and {@code getBean(String, args)},
 * See {@link Lookup @Lookup's javadoc} for details.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 2.5
 * @see #setAutowiredAnnotationType
 * @see Autowired
 * @see Value
 */
public class AutowiredAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
		implements MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

	private String requiredParameterName = "required";

	private boolean requiredParameterValue = true;

	private int order = Ordered.LOWEST_PRECEDENCE - 2;

	@Nullable
	private ConfigurableListableBeanFactory beanFactory;

	private final Set<String> lookupMethodsChecked = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

	private final Map<Class<?>, Constructor<?>[]> candidateConstructorsCache = new ConcurrentHashMap<>(256);

	private final Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);


	/**
	 * Create a new {@code AutowiredAnnotationBeanPostProcessor} for Spring's
	 * standard {@link Autowired @Autowired} annotation.
	 * <p>Also supports JSR-330's {@link javax.inject.Inject @Inject} annotation,
	 * if available.
	 */
	@SuppressWarnings("unchecked")
	public AutowiredAnnotationBeanPostProcessor() {
		this.autowiredAnnotationTypes.add(Autowired.class);
		this.autowiredAnnotationTypes.add(Value.class);
		try {
			this.autowiredAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("javax.inject.Inject", AutowiredAnnotationBeanPostProcessor.class.getClassLoader()));
			logger.info("JSR-330 'javax.inject.Inject' annotation found and supported for autowiring");
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API not available - simply skip.
		}
	}


	/**
	 * Set the 'autowired' annotation type, to be used on constructors, fields,
	 * setter methods and arbitrary config methods.
	 * <p>The default autowired annotation type is the Spring-provided {@link Autowired}
	 * annotation, as well as {@link Value}.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a member is supposed
	 * to be autowired.
	 */
	public void setAutowiredAnnotationType(Class<? extends Annotation> autowiredAnnotationType) {
		Assert.notNull(autowiredAnnotationType, "'autowiredAnnotationType' must not be null");
		this.autowiredAnnotationTypes.clear();
		this.autowiredAnnotationTypes.add(autowiredAnnotationType);
	}

	/**
	 * Set the 'autowired' annotation types, to be used on constructors, fields,
	 * setter methods and arbitrary config methods.
	 * <p>The default autowired annotation type is the Spring-provided {@link Autowired}
	 * annotation, as well as {@link Value}.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation types to indicate that a member is supposed
	 * to be autowired.
	 */
	public void setAutowiredAnnotationTypes(Set<Class<? extends Annotation>> autowiredAnnotationTypes) {
		Assert.notEmpty(autowiredAnnotationTypes, "'autowiredAnnotationTypes' must not be empty");
		this.autowiredAnnotationTypes.clear();
		this.autowiredAnnotationTypes.addAll(autowiredAnnotationTypes);
	}

	/**
	 * Set the name of a parameter of the annotation that specifies whether it is required.
	 * @see #setRequiredParameterValue(boolean)
	 */
	public void setRequiredParameterName(String requiredParameterName) {
		this.requiredParameterName = requiredParameterName;
	}

	/**
	 * Set the boolean value that marks a dependency as required
	 * <p>For example if using 'required=true' (the default), this value should be
	 * {@code true}; but if using 'optional=false', this value should be {@code false}.
	 * @see #setRequiredParameterName(String)
	 */
	public void setRequiredParameterValue(boolean requiredParameterValue) {
		this.requiredParameterValue = requiredParameterValue;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
		InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
		metadata.checkConfigMembers(beanDefinition);
	}

	/**
	 * 推断构造方法
	 * @param beanClass
	 * @param beanName
	 * @return
	 * @throws BeanCreationException
	 */
	@Override
	@Nullable
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
			throws BeanCreationException {

		// Let's check for lookup methods here...
		//查找 lookup 方法
		if (!this.lookupMethodsChecked.contains(beanName)) {
			try {
				ReflectionUtils.doWithMethods(beanClass, method -> {
					Lookup lookup = method.getAnnotation(Lookup.class);
					if (lookup != null) {
						Assert.state(this.beanFactory != null, "No BeanFactory available");
						LookupOverride override = new LookupOverride(method, lookup.value());
						try {
							RootBeanDefinition mbd = (RootBeanDefinition)
									this.beanFactory.getMergedBeanDefinition(beanName);
							mbd.getMethodOverrides().addOverride(override);
						}
						catch (NoSuchBeanDefinitionException ex) {
							throw new BeanCreationException(beanName,
									"Cannot apply @Lookup to beans without corresponding bean definition");
						}
					}
				});
			}
			catch (IllegalStateException ex) {
				throw new BeanCreationException(beanName, "Lookup method resolution failed", ex);
			}
			this.lookupMethodsChecked.add(beanName);
		}

		// Quick check on the concurrent map first, with minimal locking.
		/**
		 * 推断构造方法开始
		 * 从缓存当中 get beanClass,  为什么？
		 * 在 AnnotationConfigApplicationContext 时候，有可能会两次或多次执行 refresh 方法，
		 * 而 spring 只有一个容器，所以需要缓存起来
 		 */
		Constructor<?>[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
		if (candidateConstructors == null) {
			// Fully synchronized resolution now...
			//为什么会加上 synchronized？因为会存在多线程情况加加载spring环境
			synchronized (this.candidateConstructorsCache) {
				// 再次从缓存中获取构造方法
				candidateConstructors = this.candidateConstructorsCache.get(beanClass);
				if (candidateConstructors == null) {
					// 声明一个变量用于存储类中的构造函数
					Constructor<?>[] rawCandidates;
					try {
						// 从类中拿出声明的全部构造方法，无论是无参构造还是有参构造
						rawCandidates = beanClass.getDeclaredConstructors();
					}
					catch (Throwable ex) {
						throw new BeanCreationException(beanName,
								"Resolution of declared constructors on bean Class [" + beanClass.getName() +
								"] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
					}
					// 声明一个  candidates 变量用于存储构造方法
					List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);
					// 声明一个必要的构造函数变量
					Constructor<?> requiredConstructor = null;
					// 声明一个默认的构造函数变量
					Constructor<?> defaultConstructor = null;
					// findPrimaryConstructor 是Kotlin 专属使用对象，java可以忽略
					Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(beanClass);
					// 非混合构造函数
					int nonSyntheticConstructors = 0;
					for (Constructor<?> candidate : rawCandidates) {
						/**
						 * Synthetic 是什么？
						 * Synthetic 是 java 的一个关键字，编译器在编译过程中引用的。
						 * 非静态内部类被编译后，会在其它方法前加 Synthetic，表示是来自内部类，和外部类加以区分
						 */
						// 在这里判断构造函数是否为混合构造函数，
						// 若不是混合构造函数，则 nonSyntheticConstructors 加1，表示该构造方法可用
						if (!candidate.isSynthetic()) {
							nonSyntheticConstructors++;
						}
						else if (primaryConstructor != null) {
							continue;
						}
						/**
						 * 找构造函数上含有注解的值，@Autowired、@Value
						 */
						AnnotationAttributes ann = findAutowiredAnnotation(candidate);
						if (ann == null) {
							/**
							 * getUserClass，如果该类已经被CGLIB代理，则获取的类是父类，否则返回的是本类
							 */
							Class<?> userClass = ClassUtils.getUserClass(beanClass);
							// 判断构造方法解析出来的类与传过来的类是否是同一个类
							if (userClass != beanClass) {
								try {
									// 获取父类构造方法
									Constructor<?> superCtor =
											userClass.getDeclaredConstructor(candidate.getParameterTypes());
									ann = findAutowiredAnnotation(superCtor);
								}
								catch (NoSuchMethodException ex) {
									// Simply proceed, no equivalent superclass constructor found...
								}
							}
						}
						if (ann != null) {
							// 第一次循环时，requiredConstructor 还没有天机任何值，所以为空
							if (requiredConstructor != null) {
								throw new BeanCreationException(beanName,
										"Invalid autowire-marked constructor: " + candidate +
										". Found constructor with 'required' Autowired annotation already: " +
										requiredConstructor);
							}
							// 获取注解的值
							boolean required = determineRequiredStatus(ann);
							// 判断注解值是否为 true
							if (required) {
								// 第一次执行到这里时， candidates 为空，所以这里不成立
								if (!candidates.isEmpty()) {
									throw new BeanCreationException(beanName,
											"Invalid autowire-marked constructors: " + candidates +
											". Found constructor with 'required' Autowired annotation: " +
											candidate);
								}
								// 将构造函数赋值给 requiredConstructor
								requiredConstructor = candidate;
							}
							// 往 candidates 中添加构造函数
							candidates.add(candidate);
						}
						// 判断构造方法是否有参数，若没有参数会将这个构造方法赋值给 defaultConstructor
						else if (candidate.getParameterCount() == 0) {
							defaultConstructor = candidate;
						}
					}
					// 判断 candidates 是否为空，因为 candidates.add 只有在构造函数有注解的情况下才会被执行，
					// 所以在有注解的情况下会成立，并进入下一段代码的判断。然而 requiredConstructor 被赋值是在，注解 required == true
					// 的条件下才可以被赋值的，而 defaultConstructor 的是在构造函数没有备注解并且参数个数为 0 的情况下被赋值的。
					if (!candidates.isEmpty())  {
						// Add default constructor to list of optional constructors, as fallback.
						// 在没有被注解以及构造参数为 0 的情况下，将 defaultConstructor 存储到 candidates 列表中
						if (requiredConstructor == null) {
							if (defaultConstructor != null) {
								candidates.add(defaultConstructor);
							}
							else if (candidates.size() == 1 && logger.isWarnEnabled()) {
								logger.warn("Inconsistent constructor declaration on bean with name '" + beanName +
										"': single autowire-marked constructor flagged as optional - " +
										"this constructor is effectively required since there is no " +
										"default constructor to fall back to: " + candidates.get(0));
							}
						}
						// 将 candidates 列表中的数据放到 candidateConstructors 候选构造函数数组中
						candidateConstructors = candidates.toArray(new Constructor<?>[0]);
					}
					// 判断
					else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
						candidateConstructors = new Constructor<?>[] {rawCandidates[0]};
					}
					else if (nonSyntheticConstructors == 2 && primaryConstructor != null &&
							defaultConstructor != null && !primaryConstructor.equals(defaultConstructor)) {
						candidateConstructors = new Constructor<?>[] {primaryConstructor, defaultConstructor};
					}
					else if (nonSyntheticConstructors == 1 && primaryConstructor != null) {
						candidateConstructors = new Constructor<?>[] {primaryConstructor};
					}
					else {
						candidateConstructors = new Constructor<?>[0];
					}
					this.candidateConstructorsCache.put(beanClass, candidateConstructors);
				}
			}
		}
		return (candidateConstructors.length > 0 ? candidateConstructors : null);
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeanCreationException {

		InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
		try {
			//
			metadata.inject(bean, beanName, pvs);
		}
		catch (BeanCreationException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
		}
		return pvs;
	}

	/**
	 * 'Native' processing method for direct calls with an arbitrary target instance,
	 * resolving all of its fields and methods which are annotated with {@code @Autowired}.
	 * @param bean the target instance to process
	 * @throws BeanCreationException if autowiring failed
	 */
	public void processInjection(Object bean) throws BeanCreationException {
		Class<?> clazz = bean.getClass();
		InjectionMetadata metadata = findAutowiringMetadata(clazz.getName(), clazz, null);
		try {
			metadata.inject(bean, null, null);
		}
		catch (BeanCreationException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					"Injection of autowired dependencies failed for class [" + clazz + "]", ex);
		}
	}


	private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
		// Fall back to class name as cache key, for backwards compatibility with custom callers.
		String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
		// Quick check on the concurrent map first, with minimal locking.
		InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
		if (InjectionMetadata.needsRefresh(metadata, clazz)) {
			synchronized (this.injectionMetadataCache) {
				metadata = this.injectionMetadataCache.get(cacheKey);
				if (InjectionMetadata.needsRefresh(metadata, clazz)) {
					if (metadata != null) {
						metadata.clear(pvs);
					}
					metadata = buildAutowiringMetadata(clazz);
					this.injectionMetadataCache.put(cacheKey, metadata);
				}
			}
		}
		return metadata;
	}

	private InjectionMetadata buildAutowiringMetadata(final Class<?> clazz) {
		List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
		Class<?> targetClass = clazz;

		do {
			final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

			ReflectionUtils.doWithLocalFields(targetClass, field -> {
				AnnotationAttributes ann = findAutowiredAnnotation(field);
				if (ann != null) {
					if (Modifier.isStatic(field.getModifiers())) {
						if (logger.isWarnEnabled()) {
							logger.warn("Autowired annotation is not supported on static fields: " + field);
						}
						return;
					}
					boolean required = determineRequiredStatus(ann);
					currElements.add(new AutowiredFieldElement(field, required));
				}
			});

			ReflectionUtils.doWithLocalMethods(targetClass, method -> {
				Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
				if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
					return;
				}
				AnnotationAttributes ann = findAutowiredAnnotation(bridgedMethod);
				if (ann != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
					if (Modifier.isStatic(method.getModifiers())) {
						if (logger.isWarnEnabled()) {
							logger.warn("Autowired annotation is not supported on static methods: " + method);
						}
						return;
					}
					if (method.getParameterCount() == 0) {
						if (logger.isWarnEnabled()) {
							logger.warn("Autowired annotation should only be used on methods with parameters: " +
									method);
						}
					}
					boolean required = determineRequiredStatus(ann);
					PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
					currElements.add(new AutowiredMethodElement(method, required, pd));
				}
			});

			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return new InjectionMetadata(clazz, elements);
	}

	/**
	 * autowiredAnnotationTypes 中存放的是 Autowired 和 value
	 * AnnotationAttributes 存放的是注解属性的值，例如@Autowired 中的 required
	 * 		public @interface Autowired{boolean required() default true}
	 *
	 * @param ao
	 * @return
	 */
	@Nullable
	private AnnotationAttributes findAutowiredAnnotation(AccessibleObject ao) {
		//  判断注解个数是否大于 0
		if (ao.getAnnotations().length > 0) {  // autowiring annotations have to be local
			for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
				AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ao, type);
				if (attributes != null) {
					return attributes;
				}
			}
		}
		return null;
	}

	/**
	 * Determine if the annotated field or method requires its dependency.
	 * <p>A 'required' dependency means that autowiring should fail when no beans
	 * are found. Otherwise, the autowiring process will simply bypass the field
	 * or method when no beans are found.
	 * @param ann the Autowired annotation
	 * @return whether the annotation indicates that a dependency is required
	 */
	protected boolean determineRequiredStatus(AnnotationAttributes ann) {
		return (!ann.containsKey(this.requiredParameterName) ||
				this.requiredParameterValue == ann.getBoolean(this.requiredParameterName));
	}

	/**
	 * Obtain all beans of the given type as autowire candidates.
	 * @param type the type of the bean
	 * @return the target beans, or an empty Collection if no bean of this type is found
	 * @throws BeansException if bean retrieval failed
	 */
	protected <T> Map<String, T> findAutowireCandidates(Class<T> type) throws BeansException {
		if (this.beanFactory == null) {
			throw new IllegalStateException("No BeanFactory configured - " +
					"override the getBeanOfType method or specify the 'beanFactory' property");
		}
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type);
	}

	/**
	 * Register the specified bean as dependent on the autowired beans.
	 */
	private void registerDependentBeans(@Nullable String beanName, Set<String> autowiredBeanNames) {
		if (beanName != null) {
			for (String autowiredBeanName : autowiredBeanNames) {
				if (this.beanFactory != null && this.beanFactory.containsBean(autowiredBeanName)) {
					this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Autowiring by type from bean name '" + beanName +
							"' to bean named '" + autowiredBeanName + "'");
				}
			}
		}
	}

	/**
	 * Resolve the specified cached method argument or field value.
	 */
	@Nullable
	private Object resolvedCachedArgument(@Nullable String beanName, @Nullable Object cachedArgument) {
		if (cachedArgument instanceof DependencyDescriptor) {
			DependencyDescriptor descriptor = (DependencyDescriptor) cachedArgument;
			Assert.state(this.beanFactory != null, "No BeanFactory available");
			return this.beanFactory.resolveDependency(descriptor, beanName, null, null);
		}
		else {
			return cachedArgument;
		}
	}


	/**
	 * Class representing injection information about an annotated field.
	 */
	private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

		private final boolean required;

		private volatile boolean cached = false;

		@Nullable
		private volatile Object cachedFieldValue;

		public AutowiredFieldElement(Field field, boolean required) {
			super(field, null);
			this.required = required;
		}

		@Override
		protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
			Field field = (Field) this.member;
			Object value;
			// 判断是否有缓存
			if (this.cached) {
				value = resolvedCachedArgument(beanName, this.cachedFieldValue);
			}
			else {
				// 获取 field 的依赖属性, filed = private com.muqie.service.UserService com.muqie.service.OrderService.userService
				// desc = "userService"
				DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
				desc.setContainingClass(bean.getClass());
				Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
				Assert.state(beanFactory != null, "No BeanFactory available");
				TypeConverter typeConverter = beanFactory.getTypeConverter();
				try {
					// 很重要，对依赖进行转换，实质上是从 beanFactory 中取
					value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);
				}
				catch (BeansException ex) {
					throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex);
				}
				synchronized (this) {
					if (!this.cached) {
						if (value != null || this.required) {
							this.cachedFieldValue = desc;
							registerDependentBeans(beanName, autowiredBeanNames);
							if (autowiredBeanNames.size() == 1) {
								String autowiredBeanName = autowiredBeanNames.iterator().next();
								//  在该方法会调用 getSigleton 方法，此时 getSigleton  方法中的
								//  singletonObject = this.earlySingletonObjects.get(beanName); 会存在bean，
								//  但是该 bean 并没有被注入属性
								if (beanFactory.containsBean(autowiredBeanName) &&
										beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
									this.cachedFieldValue = new ShortcutDependencyDescriptor(
											desc, autowiredBeanName, field.getType());
								}
							}
						}
						else {
							this.cachedFieldValue = null;
						}
						this.cached = true;
					}
				}
			}
			if (value != null) {
				ReflectionUtils.makeAccessible(field);
				field.set(bean, value);
			}
		}
	}


	/**
	 * Class representing injection information about an annotated method.
	 */
	private class AutowiredMethodElement extends InjectionMetadata.InjectedElement {

		private final boolean required;

		private volatile boolean cached = false;

		@Nullable
		private volatile Object[] cachedMethodArguments;

		public AutowiredMethodElement(Method method, boolean required, @Nullable PropertyDescriptor pd) {
			super(method, pd);
			this.required = required;
		}

		@Override
		protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
			if (checkPropertySkipping(pvs)) {
				return;
			}
			Method method = (Method) this.member;
			Object[] arguments;
			if (this.cached) {
				// Shortcut for avoiding synchronization...
				arguments = resolveCachedArguments(beanName);
			}
			else {
				Class<?>[] paramTypes = method.getParameterTypes();
				arguments = new Object[paramTypes.length];
				DependencyDescriptor[] descriptors = new DependencyDescriptor[paramTypes.length];
				Set<String> autowiredBeans = new LinkedHashSet<>(paramTypes.length);
				Assert.state(beanFactory != null, "No BeanFactory available");
				TypeConverter typeConverter = beanFactory.getTypeConverter();
				for (int i = 0; i < arguments.length; i++) {
					MethodParameter methodParam = new MethodParameter(method, i);
					DependencyDescriptor currDesc = new DependencyDescriptor(methodParam, this.required);
					currDesc.setContainingClass(bean.getClass());
					descriptors[i] = currDesc;
					try {
						// 很重要，解决依赖关系
						Object arg = beanFactory.resolveDependency(currDesc, beanName, autowiredBeans, typeConverter);
						if (arg == null && !this.required) {
							arguments = null;
							break;
						}
						arguments[i] = arg;
					}
					catch (BeansException ex) {
						throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(methodParam), ex);
					}
				}
				synchronized (this) {
					if (!this.cached) {
						if (arguments != null) {
							Object[] cachedMethodArguments = new Object[paramTypes.length];
							System.arraycopy(descriptors, 0, cachedMethodArguments, 0, arguments.length);
							registerDependentBeans(beanName, autowiredBeans);
							if (autowiredBeans.size() == paramTypes.length) {
								Iterator<String> it = autowiredBeans.iterator();
								for (int i = 0; i < paramTypes.length; i++) {
									String autowiredBeanName = it.next();
									if (beanFactory.containsBean(autowiredBeanName) &&
											beanFactory.isTypeMatch(autowiredBeanName, paramTypes[i])) {
										cachedMethodArguments[i] = new ShortcutDependencyDescriptor(
												descriptors[i], autowiredBeanName, paramTypes[i]);
									}
								}
							}
							this.cachedMethodArguments = cachedMethodArguments;
						}
						else {
							this.cachedMethodArguments = null;
						}
						this.cached = true;
					}
				}
			}
			if (arguments != null) {
				try {
					ReflectionUtils.makeAccessible(method);
					method.invoke(bean, arguments);
				}
				catch (InvocationTargetException ex) {
					throw ex.getTargetException();
				}
			}
		}

		@Nullable
		private Object[] resolveCachedArguments(@Nullable String beanName) {
			Object[] cachedMethodArguments = this.cachedMethodArguments;
			if (cachedMethodArguments == null) {
				return null;
			}
			Object[] arguments = new Object[cachedMethodArguments.length];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = resolvedCachedArgument(beanName, cachedMethodArguments[i]);
			}
			return arguments;
		}
	}


	/**
	 * DependencyDescriptor variant with a pre-resolved target bean name.
	 */
	@SuppressWarnings("serial")
	private static class ShortcutDependencyDescriptor extends DependencyDescriptor {

		private final String shortcut;

		private final Class<?> requiredType;

		public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut, Class<?> requiredType) {
			super(original);
			this.shortcut = shortcut;
			this.requiredType = requiredType;
		}

		@Override
		public Object resolveShortcut(BeanFactory beanFactory) {
			return beanFactory.getBean(this.shortcut, this.requiredType);
		}
	}

}
