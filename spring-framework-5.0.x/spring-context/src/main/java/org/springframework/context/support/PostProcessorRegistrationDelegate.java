/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();
		// beanFactory 是 DefaultListableBeanFactory ，是 BeanDefinitionRegistry 的实现类，所以肯定满足if
		// DefaultListableBeanFactory implements ConfigurableListableBeanFactory
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			/**
			 * 这里为何定义两个 list？
			 * BeanDefinitionRegistryPostProcessor 继承了 BeanFactoryPostProcessor
			 * regularPostProcessors 存放的是手动添加的 BeanFactoryPostProcessor 的其它子类
			 * registryProcessors 存放的是 BeanDefinitionRegistryPostProcessor 这个类型的类
			 */
			// regularPostProcessors 用来存放 BeanFactoryPostProcessor，
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// registryProcessors 用来存放的是 BeanDefinitionRegistryPostProcessor
			// BeanDefinitionRegistryPostProcessor 扩展了 BeanFactoryPostProcessor
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
			/**
			 * 循环自己定义的 beanFactoryPostProcessor，正常情况下，beanFactoryPostProcessors 肯定没有数据
			 * 因为 beanFactoryPostProcessors 是手动添加的，在执行
			 * annotationConfigApplicationContext.addBeanFactoryPostProcessor(XXX)时会有数据
			 */
			// 自定义的 beanFactoryPostProcessors
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				/**
				 * 判断 postProcessor 是不是 BeanFactoryPostProcessor 类型，因为 BeanDefinitionRegistryPostProcessor
				 * 是 BeanFactoryPostProcessor 的扩展类（public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor）。
				 * 所以在这里判断是否是 BeanDefinitionRegistryPostProcessor 类型，若是这执行 registryProcessor.postProcessBeanDefinitionRegistry(registry)，
				 * 然后将对象装到 registryProcessors
				 * 既，若是 BeanDefinitionRegistryPostProcessor 类型的子类就放到 registryProcessors中，若是 其它子类就放到 regularPostProcessors 中
				 */
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				else {
					//不是的话，就装到 regularPostProcessors
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			// 这个 currentRegistryProcessors 放的是 spring 内部自己实现了BeanDefinitionRegistryPostProcessor 接口的对象
			// 目前只有一个后置处理器 ConfigurationClassPostProcessor
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			/**
			 * 获得实现 BeanDefinitionRegistryPostProcessor 接口的类的 BeanName:org.springframework.context.annotation.internalConfigurationAnnotationProcessor
			 * 并且装入数组 postProcessorNames ，一般情况下，只会找到一个。
			 * 这里又有一个坑？
			 * 为什么我自己创建了一个实现 BeanDefinitionRegistryPostProcessor 接口的类，也打上了 @Component 注解
			 * 配置类也加上了 @Component 注解，但是这里却没有拿到。
			 * 因为知道这一步，Spring 还没有去扫描，所以加了 @Component 注解的类没有加载进来。
			 * 而扫描是在 ConfigurationClassPostProcessor 类中完成的，也就是下面的第一个
			 * invokeBeanDefinitionRegistryPostProcessors 方法
			 */
			// getBeanNamesForType 根据 bean 的类型获取 bean 的名字，这里的 type 指的是 bd 当中描述当前类的 class 类型
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			/**
			 * 这个地方可以得到一个 BeanFactoryPostProcessor ,因为是 spring 默认在最开始自己注册的，
			 * 为什么自己在最开始注册呢？
			 * 因为 spring 的工厂需要解析去扫描等功能。而这些功能都是需要在 spring 工厂初始化完成之前执行。
			 * 所以要么在工厂最开始的时候、要么在工厂初始化之中，反正不能再之后。
			 * 如果在之后就没有意义，因为这个时候已经需要工厂了，所以这里 spring 在一开始就注册了一个 BeanFactoryPostProcessor,
			 * 用来插手 springFactory 的创建过程。
			 * 在这个地方断点可以知道这个类叫做 ConfigurationClassPostProcessor
			 */
			/**
			 * 循环 postProcessorNames，其实也就是 org.springframework.context.annotation.internalConfigurationAnnotationProcessor，
			 * 判断此后置处理器是否实现了 PriorityOrdered 接口（ConfigurationAnnotationProcessor 也实现了
			 * PriorityOrdered接口），如果实现了，把它添加到 currentRegistryProcessors 这个临时变量中，
			 * 再放入 processedBeans，代表这个后置处理已经被处理过了。当然现在还没有处理，但是马上就要处理了。。。
			 */
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			// 排序不重要，况且 currentRegistryProcessors 这里也只有一个数据
			// 当然目前这里只有一个后置处理器，就是 ConfigurationClassPostProcessor
			sortPostProcessors(currentRegistryProcessors, beanFactory);

			/**
			 * 把 currentRegistryProcessors 合并到 registryProcessors，
			 * 为什么需要合并？因为一开始 spring 只会执行 BeanDefinitionRegistryPostProcessor 独有的方法，
			 * 而不会执行 BeanDefinitionRegistryPostProcessor 父类的方法，即 BeanFactoryProcessor 接口中的方法，
			 * 所以需要把这些后置处理器放入一个集合中，后续统一执行 BeanFactoryProcessor 接口中的方法。
			 * 当然目前这里只有一个后置处理器，就是 ConfigurationClassPostProcessor
			 */
			//合并list，不重要(为什么要合并，因为还有spring自己注册的bean)
			registryProcessors.addAll(currentRegistryProcessors);

			/**
			 * 可以理解为执行 currentRegistryProcessors 中的 ConfigurationClassPostProcessor 中的
			 * postProcessBeanDefinitionRegistry 方法，这就是 Spring 设计思想的体现了，
			 * 在这里体现的就是其中的热插拔，插件化开发的思想。Spring 中很多东西都是交给插件去处理的，
			 * 这个后置处理器就相当于一个插件，如果不想用了，直接不添加就是了。这个方法特别重要.
			 */
			// 很重要
			// 执行所有的 BeanDefinitionRegistryPostProcessor，这里目前就只有一个类 ConfigurationClassPostProcessor.class
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);

			// 以上代码执行完成了所有的 BeanDefninitionRegistryPostProcessor


			// 这个list只是一个临时变量，故而要清除，以方便下面用到
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			/**
			 * 再次根据 BeanDefinitionRegistryPostProcessor 获得 BeanName，看这个 BeanName 是否已经被执行过了，
			 * 有没有实现 Ordered 接口。如果没有被执行过，也实现了 Ordered 接口的话，把对象推送到 currentRegistryProcessors，
			 * 名称推送到 processedBeans，如果没有实现 Ordered 接口的话，这里不把数据加到 currentRegistryProcessors，
			 * processedBeans中，后续再做处理。
			 * 这里才可以获得我们定义的实现了 BeanDefinitionRegistryPostProcessor 的 Bean。
			 *
			 *
			 * 这里就可以获得我们定义的，并且打上 @Component 注解的后置处理器了，因为 Spring 已经完成了扫描，
			 * 但是这里需要注意的是，由于 ConfigurationClassPostProcessor 在上面已经被执行过了，
			 * 所以虽然可以通过 getBeanNamesForType 获得，但是并不会加入到 currentRegistryProcessors 和
			 * processedBeans 。
			 */
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			// 处理排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 合并 Processors
			registryProcessors.addAll(currentRegistryProcessors);
			// 执行所有的 BeanDefinitionRegistryPostProcessor
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 清空临时变量
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.

			// 上面的代码是执行了实现了 Ordered 接口的 BeanDefinitionRegistryPostProcessor，
			// 下面的代码就是执行没有实现 Ordered 接口的 BeanDefinitionRegistryPostProcessor
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 执行 BeanFactoryPostProcessor 的回调
			// 前面执行的 BeanFactoryPostProcessor 的子类 BeanDefinitionRegistryPostProcessor 的回调
			// 这里执行的是 BeanFactoryPostProcessor
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);

			//执行自己定义的BeanFactoryPostProcessor，regularPostProcessors中存放的是自定义的BeanFactory
			/**
			 * regularPostProcessors 装载 BeanFactoryPostProcessor ，执行 BeanFactoryPostProcessor 的方法
			 *但是regularPostProcessors一般情况下，是不会有数据的，只有在外面手动添加BeanFactoryPostProcessor，才会有数据
			 * ConfigurationClassPostProcessor
			 */
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		// 找到 BeanFactoryPostProcessor 实现类的 BeanName 数组
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		//循环 BeanName 数组
		for (String ppName : postProcessorNames) {
			//如果这个Bean被执行过了，跳过
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			//如果实现了 PriorityOrdered 接口，加入到 priorityOrderedPostProcessors
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			//如果实现了Ordered接口，加入到orderedPostProcessorNames
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			//如果既没有实现PriorityOrdered，也没有实现Ordered。加入到nonOrderedPostProcessorNames
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		//排序处理 priorityOrderedPostProcessors，即实现了PriorityOrdered 接口的 BeanFactoryPostProcessor
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		//执行priorityOrderedPostProcessors
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);
		//执行实现了Ordered接口的BeanFactoryPostProcessor
		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// 执行既没有实现PriorityOrdered接口，也没有实现Ordered接口的BeanFactoryPostProcessor
		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {


		// 从 beanDefinitionMap 中根据类型找出找到所有的 BeanPostProcessor，
		// beanFactory 为 DefaultListableBeanFactory 类型，DefaultListableBeanFactory 实现了 ConfigurableListableBeanFactory接口
 		// 代码运行到这，会得到三个类 ApplicationContextAwareProcessor、ApplicationListenerDetector、ImportAwareBeanPostProcessor,这三个类都实现了 BeanPostProcessor 接口
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		//
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		/**
		 * CommonAnnotationBeanPostProcessor 主要处理一些公共的注解类，例如：@PostConstruct
		 * AutowiredAnnotationBeanPostProcessor 处理 @Autowired 注解的类
		 * RequiredAnnotationBeanPostProcessor 处理 @Required 注解的类
		 * 这里并没有执行 BeanPostProcessor,只是将 BeanPostProcessor 放到一个数组中，以便以后处理
		 */
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {
		// 循环的其实是一个类，ConfigurationClassPostProcessor.class
		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			// 在这里该方法 postProcessBeanDefinitionRegistry() 为 ConfigurationClassPostProcessor 的方法
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 *
	 * 当 spring 的配置中的后置处理器还没有被注册就已经开始了bean 的初始化
	 * 便会打印出 BeanPostProcessorChecker 中设定的信息
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
