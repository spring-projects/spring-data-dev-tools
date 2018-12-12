/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.release.build;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.junit.Test;
import org.springframework.data.release.model.Iteration;
import org.springframework.data.release.model.Module;
import org.springframework.data.release.model.ModuleIteration;
import org.springframework.data.release.model.Phase;
import org.springframework.data.release.model.Projects;
import org.springframework.data.release.model.Train;
import org.springframework.data.release.model.TrainIteration;
import org.springframework.data.release.model.Version;
import org.springframework.data.release.utils.Logger;
import org.xmlbeam.XBProjector;
import org.xmlbeam.config.DefaultXMLFactoriesConfig;
import org.xmlbeam.config.DefaultXMLFactoriesConfig.NamespacePhilosophy;
import org.xmlbeam.io.XBStreamInput;

/**
 * @author Mark Paluch
 */
public class PomUpdaterUnitTests {

	Module module = Module.create(Projects.R2DBC, Version.of(1, 0));
	XBProjector projectionFactory = new XBProjector(
			new DefaultXMLFactoriesConfig().setNamespacePhilosophy(NamespacePhilosophy.AGNOSTIC));

	/**
	 * @see #85
	 */
	@Test
	public void shouldUpdateStandaloneVersions() throws IOException {

		ModuleIteration moduleIteration = new ModuleIteration(module,
				new TrainIteration(new Train("Moore", module).withDetached(true), Iteration.M1));

		UpdateInformation updateInformation = UpdateInformation.of(TrainIteration.from(moduleIteration), Phase.PREPARE);

		PomUpdater pomUpdater = new PomUpdater(new Logger(), updateInformation, module.getProject());

		XBStreamInput stream = projectionFactory.io().stream(getClass().getResourceAsStream("/r2dbc-pom.xml"));

		Pom pom = stream.read(Pom.class);
		pomUpdater.updateDependencyProperties(pom);

		assertThat(pom.getProperty("springdata.relational")).isEqualTo("1.1.0.M1");
		assertThat(pom.getProperty("springdata.commons")).isEqualTo("2.2.0.M1");
	}
}
