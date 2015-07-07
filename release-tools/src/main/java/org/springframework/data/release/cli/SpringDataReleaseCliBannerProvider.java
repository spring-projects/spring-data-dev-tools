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
package org.springframework.data.release.cli;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.BannerProvider;
import org.springframework.shell.support.util.FileUtils;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
class SpringDataReleaseCliBannerProvider implements BannerProvider {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.shell.plugin.NamedProvider#getProviderName()
	 */
	@Override
	public String getProviderName() {
		return "Spring Data Release Shell";
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.shell.plugin.BannerProvider#getBanner()
	 */
	@Override
	public String getBanner() {

		StringBuilder builder = new StringBuilder();

		builder.append(FileUtils.readBanner(SpringDataReleaseCliBannerProvider.class, "banner.txt"));
		builder.append(getVersion()).append(OsUtils.LINE_SEPARATOR);
		builder.append(OsUtils.LINE_SEPARATOR);

		return builder.toString();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.shell.plugin.BannerProvider#getVersion()
	 */
	@Override
	public String getVersion() {
		return "1.0";
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.shell.plugin.BannerProvider#getWelcomeMessage()
	 */
	@Override
	public String getWelcomeMessage() {
		return "Welcome to the Spring Data Release Shell!";
	}

}
