/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.release.maven;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.release.model.ArtifactVersion;
import org.xmlbeam.ProjectionFactory;
import org.xmlbeam.XBProjector;
import org.xmlbeam.XBProjector.Flags;
import org.xmlbeam.config.DefaultXMLFactoriesConfig;
import org.xmlbeam.config.DefaultXMLFactoriesConfig.NamespacePhilosophy;
import org.xmlbeam.types.DefaultTypeConverter;
import org.xmlbeam.types.TypeConverter;

/**
 * @author Oliver Gierke
 */
@Configuration
class MavenConfig {

	@Bean
	public ProjectionFactory projectionFactory() {

		TypeConverter converter = new DefaultTypeConverter().setConversionForType(ArtifactVersion.class,
				new ArtifactVersionConverter());

		DefaultXMLFactoriesConfig config = new DefaultXMLFactoriesConfig();
		config.setNamespacePhilosophy(NamespacePhilosophy.AGNOSTIC);

		XBProjector projector = new XBProjector(config, Flags.TO_STRING_RENDERS_XML);
		projector.config().setTypeConverter(converter);

		return projector;
	}

	/**
	 * Custom converter to be able to use {@link ArtifactVersion} directly from within an XmlBeam projection.
	 * 
	 * @author Oliver Gierke
	 */
	private static class ArtifactVersionConverter extends DefaultTypeConverter.Conversion<ArtifactVersion> {

		private static final long serialVersionUID = 1L;

		public ArtifactVersionConverter() {
			super(null);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.xmlbeam.types.DefaultTypeConverter.Conversion#convert(java.lang.String)
		 */
		@Override
		public ArtifactVersion convert(String data) {
			return ArtifactVersion.parse(data);
		}
	}
}
