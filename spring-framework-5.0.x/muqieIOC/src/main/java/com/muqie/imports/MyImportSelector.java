package com.muqie.imports;

import com.muqie.dao.IndexDao2;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class MyImportSelector implements ImportSelector {
	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		//importingClassMetadata.getMetaAnnotationTypes("");
		return new String[]{IndexDao2.class.getName()};
	}
}
