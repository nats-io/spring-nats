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

package io.nats.cloud.stream.binder;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

class NatsBinderServerConfiguredCondition implements Condition {
    private static final String BINDER_SERVER_PROPERTY = "nats.spring.cloud.stream.binder.server";
    private static final String GLOBAL_SERVER_PROPERTY = "nats.spring.server";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return hasText(context, BINDER_SERVER_PROPERTY) || hasText(context, GLOBAL_SERVER_PROPERTY);
    }

    private static boolean hasText(ConditionContext context, String property) {
        return StringUtils.hasText(context.getEnvironment().getProperty(property));
    }
}
