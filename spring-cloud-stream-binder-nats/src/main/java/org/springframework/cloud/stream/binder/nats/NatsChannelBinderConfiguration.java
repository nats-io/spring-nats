/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.stream.binder.nats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.config.BindingServiceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.config.EnableIntegration;

@Configuration
@ConditionalOnMissingBean(Binder.class)
@EnableIntegration
public class NatsChannelBinderConfiguration<T> {

	public static final String NAME = "integration";

	/**
	 * Utility operation to return an array of configuration classes defined in
	 * {@link EnableBinding} annotation. Typically used for tests that do not rely on
	 * creating an SCSt boot application annotated with {@link EnableBinding}, yet require
	 * full {@link Binder} configuration.
	 * @param additionalConfigurationClasses config classes to be added to the default
	 * config
	 * @return an array of configuration classes defined in {@link EnableBinding}
	 * annotation
	 */
	public static Class<?>[] getCompleteConfiguration(
			Class<?>... additionalConfigurationClasses) {
		List<Class<?>> configClasses = new ArrayList<>();
		configClasses.add(NatsChannelBinderConfiguration.class);
		Import annotation = AnnotationUtils.getAnnotation(EnableBinding.class,
				Import.class);
		Map<String, Object> annotationAttributes = AnnotationUtils
				.getAnnotationAttributes(annotation);
		configClasses
				.addAll(Arrays.asList((Class<?>[]) annotationAttributes.get("value")));
		configClasses.add(BindingServiceConfiguration.class);
		if (additionalConfigurationClasses != null) {
			configClasses.addAll(Arrays.asList(additionalConfigurationClasses));
		}
		return configClasses.toArray(new Class<?>[] {});
	}

	@SuppressWarnings("unchecked")
	@Bean
	public Binder<T, ? extends ConsumerProperties, ? extends ProducerProperties> springIntegrationChannelBinder(
			NatsChannelBinderProvisioner provisioner) {
		return (Binder<T, ? extends ConsumerProperties, ? extends ProducerProperties>) new NatsChannelBinder(
				provisioner);
	}

	@Bean
	public NatsChannelBinderProvisioner springIntegrationProvisioner() {
		return new NatsChannelBinderProvisioner();
	}

}