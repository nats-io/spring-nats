/*
 * Copyright 2013-2019 the original author or authors.
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

import io.nats.client.Connection;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.DefaultPollableMessageSource;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.binder.nats.properties.NatsBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsConsumerProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsExtendedBindingProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsProducerProperties;
import org.springframework.cloud.stream.binder.nats.provisioning.NatsSubjectProvisioner;
import org.springframework.cloud.stream.config.MessageSourceCustomizer;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

//import org.springframework.messaging.converter.AbstractMessageConverter;
//import org.springframework.messaging.support.ErrorMessage;
//import org.springframework.util.StringUtils;
//import org.springframework.messaging.MessagingException;
//import org.springframework.core.task.SimpleAsyncTaskExecutor;
//import org.springframework.cloud.stream.binder.nats.properties.NatsCommonProperties;
//import org.springframework.integration.channel.AbstractMessageChannel;
//import org.springframework.cloud.stream.binder.BinderHeaders;
//import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
//import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
//import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
//import org.springframework.cloud.stream.binder.HeaderMode;
//import org.springframework.cloud.stream.binder.nats.properties.NatsCommonProperties;
//import org.springframework.cloud.stream.binder.nats.properties.NatsConsumerProperties;
//import org.springframework.cloud.stream.binder.nats.properties.NatsExtendedBindingProperties;
//import org.springframework.cloud.stream.binder.nats.properties.NatsProducerProperties;
//import org.springframework.cloud.stream.binder.nats.provisioning.NatsExchangeQueueProvisioner;
//import org.springframework.expression.Expression;
//import org.springframework.expression.spel.standard.SpelExpressionParser;
//import org.springframework.integration.StaticMessageHeaderAccessor;
//import org.springframework.integration.acks.AcknowledgmentCallback;
//import org.springframework.integration.acks.AcknowledgmentCallback.Status;
//import org.springframework.integration.channel.AbstractMessageChannel;
//import org.springframework.integration.channel.DirectChannel;
//import org.springframework.integration.context.IntegrationContextUtils;
//import org.springframework.integration.support.DefaultErrorMessageStrategy;
//import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
//import org.springframework.retry.RetryPolicy;
//import org.springframework.retry.backoff.ExponentialBackOffPolicy;
//import org.springframework.retry.policy.SimpleRetryPolicy;
//import org.springframework.retry.support.RetryTemplate;
//import org.springframework.scheduling.TaskScheduler;
//import org.springframework.context.support.GenericApplicationContext;
/**
 * A {@link org.springframework.cloud.stream.binder.Binder} implementation backed by
 * the NATS.io messaging system.
 *
 * @author The NATS Authors
 */
// @checkstyle:off
public class NatsMessageChannelBinder extends
		AbstractMessageChannelBinder<ExtendedConsumerProperties<NatsConsumerProperties>, ExtendedProducerProperties<NatsProducerProperties>, NatsSubjectProvisioner>
		implements
		ExtendedPropertiesBinder<MessageChannel, NatsConsumerProperties, NatsProducerProperties>,
		DisposableBean {

	// @checkstyle:on

	private NatsExtendedBindingProperties extendedBindingProperties = new NatsExtendedBindingProperties();
	NatsBinderConfigurationProperties configuration;
	// TODO:  Add NatsTemplate

	public NatsMessageChannelBinder(NatsBinderConfigurationProperties configuration,
		NatsSubjectProvisioner provisioningProvider) {

		this(configuration, provisioningProvider, null);
	}

	public NatsMessageChannelBinder(
		NatsBinderConfigurationProperties configuration,
		NatsSubjectProvisioner provisioningProvider,
		MessageSourceCustomizer<NatsMessageSource> sourceCustomizer) {

		super(new String[0], provisioningProvider, null, sourceCustomizer);
		Assert.notNull(configuration, "configuration must not be null");
		this.configuration = configuration;
	}

	public void setExtendedBindingProperties(
			NatsExtendedBindingProperties extendedBindingProperties) {
		this.extendedBindingProperties = extendedBindingProperties;
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();
		// String[] addresses = StringUtils.commaDelimitedListToStringArray(
		// 		this.natsProperties.getAddresses());

		// this.connectionFactory = new LocalizedQueueConnectionFactory(
		// this.connectionFactory, addresses, this.adminAddresses, this.nodes,
		// this.natsProperties.getVirtualHost(),
		// this.natsProperties.getUsername(),
		// this.natsProperties.getPassword(),
		// this.natsProperties.getSsl().isEnabled(),
		// this.natsProperties.getSsl().getKeyStore(),
		// this.natsProperties.getSsl().getTrustStore(),
		// this.natsProperties.getSsl().getKeyStorePassword(),
		// this.natsProperties.getSsl().getTrustStorePassword());
	}

	@Override
	public void destroy() throws Exception {
		// Destroy connection?  Connection factory?
	}

	@Override
	public NatsConsumerProperties getExtendedConsumerProperties(String channelName) {
		return this.extendedBindingProperties.getExtendedConsumerProperties(channelName);
	}

	@Override
	public NatsProducerProperties getExtendedProducerProperties(String channelName) {
		return this.extendedBindingProperties.getExtendedProducerProperties(channelName);
	}

	@Override
	public String getDefaultsPrefix() {
		return this.extendedBindingProperties.getDefaultsPrefix();
	}

	@Override
	public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
		return this.extendedBindingProperties.getExtendedPropertiesEntryClass();
	}

	@Override
	protected MessageHandler createProducerMessageHandler(
			final ProducerDestination producerDestination,
			ExtendedProducerProperties<NatsProducerProperties> producerProperties,
			MessageChannel errorChannel) {
		return new NatsMessageHandler(producerDestination.getName(),
			producerProperties);

		/*
		String prefix = producerProperties.getExtension().getPrefix();
		String destination = producerDestination.getName();
		NatsProducerProperties extendedProperties = producerProperties.getExtension();
		endpoint.setDefaultDeliveryMode(extendedProperties.getDeliveryMode());
		endpoint.setBeanFactory(this.getBeanFactory());
		if (errorChannel != null) {
			checkConnectionFactoryIsErrorCapable();
			endpoint.setReturnChannel(errorChannel);
			endpoint.setConfirmNackChannel(errorChannel);
			String ackChannelBeanName = StringUtils
					.hasText(extendedProperties.getConfirmAckChannel())
							? extendedProperties.getConfirmAckChannel()
							: IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME;
			if (!ackChannelBeanName.equals(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME)
					&& !getApplicationContext().containsBean(ackChannelBeanName)) {
				GenericApplicationContext context = (GenericApplicationContext) getApplicationContext();
				context.registerBean(ackChannelBeanName, DirectChannel.class,
						() -> new DirectChannel());
			}
			endpoint.setConfirmAckChannelName(ackChannelBeanName);
			endpoint.setConfirmCorrelationExpressionString("#root");
			endpoint.setErrorMessageStrategy(new DefaultErrorMessageStrategy());
		}
		endpoint.setHeadersMappedLast(true);
		return endpoint;
		*/
	}

	@Override
	protected void postProcessOutputChannel(MessageChannel outputChannel,
			ExtendedProducerProperties<NatsProducerProperties> producerProperties) {
		// TODO?
	}

	@Override
	protected MessageProducer createConsumerEndpoint(
			ConsumerDestination consumerDestination, String group,
			ExtendedConsumerProperties<NatsConsumerProperties> properties) {

		Connection connection = null;
		MessageProducer mp = new NatsInboundMessageProducer(connection, consumerDestination.getName());

		// TODO - do we need the following?
		// mp.setBeanFactory(this.getBeanFactory())
		// mp.setBeanName(this.getBeanFactory().getName());
		// mp.setErrorMessageStrategy(errorMessageStrategy);
	    // mp.setErrorChannel(errorInfrastructure.getErrorChannel());
		// mp.setMessageConverter(passThoughConverter);

		return mp;
		/*
		boolean directContainer = properties.getExtension().getContainerType()
				.equals(ContainerType.DIRECT);
		AbstractMessageListenerContainer listenerContainer = directContainer
				? new DirectMessageListenerContainer(this.connectionFactory)
				: new SimpleMessageListenerContainer(this.connectionFactory);
		listenerContainer
				.setAcknowledgeMode(properties.getExtension().getAcknowledgeMode());
		listenerContainer.setChannelTransacted(properties.getExtension().isTransacted());
		listenerContainer
				.setDefaultRequeueRejected(properties.getExtension().isRequeueRejected());
		int concurrency = properties.getConcurrency();
		concurrency = concurrency > 0 ? concurrency : 1;
		if (directContainer) {
			setDMLCProperties(properties,
					(DirectMessageListenerContainer) listenerContainer, concurrency);
		}
		else {
			setSMLCProperties(properties,
					(SimpleMessageListenerContainer) listenerContainer, concurrency);
		}
		listenerContainer.setPrefetchCount(properties.getExtension().getPrefetch());
		listenerContainer
				.setRecoveryInterval(properties.getExtension().getRecoveryInterval());
		listenerContainer.setTaskExecutor(
				new SimpleAsyncTaskExecutor(consumerDestination.getName() + "-"));
		String[] queues = StringUtils.tokenizeToStringArray(destination, ",", true, true);
		listenerContainer.setQueueNames(queues);
		listenerContainer.setAfterReceivePostProcessors(this.decompressingPostProcessor);
		listenerContainer.setMessagePropertiesConverter(
				NatsMessageChannelBinder.inboundMessagePropertiesConverter);
		listenerContainer.setExclusive(properties.getExtension().isExclusive());
		listenerContainer
				.setMissingQueuesFatal(properties.getExtension().getMissingQueuesFatal());
		if (properties.getExtension().getFailedDeclarationRetryInterval() != null) {
			listenerContainer.setFailedDeclarationRetryInterval(
					properties.getExtension().getFailedDeclarationRetryInterval());
		}
		if (getApplicationEventPublisher() != null) {
			listenerContainer
					.setApplicationEventPublisher(getApplicationEventPublisher());
		}
		else if (getApplicationContext() != null) {
			listenerContainer.setApplicationEventPublisher(getApplicationContext());
		}
		getContainerCustomizer().configure(listenerContainer,
				consumerDestination.getName(), group);
		if (StringUtils.hasText(properties.getExtension().getConsumerTagPrefix())) {
			final AtomicInteger index = new AtomicInteger();
			listenerContainer.setConsumerTagStrategy(
					q -> properties.getExtension().getConsumerTagPrefix() + "#"
							+ index.getAndIncrement());
		}
		listenerContainer.afterPropertiesSet();

		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(
				listenerContainer);
		adapter.setBeanFactory(this.getBeanFactory());
		adapter.setBeanName("inbound." + destination);
		DefaultAmqpHeaderMapper mapper = DefaultAmqpHeaderMapper.inboundMapper();
		mapper.setRequestHeaderNames(properties.getExtension().getHeaderPatterns());
		adapter.setHeaderMapper(mapper);
		ErrorInfrastructure errorInfrastructure = registerErrorInfrastructure(
				consumerDestination, group, properties);
		if (properties.getMaxAttempts() > 1) {
			adapter.setRetryTemplate(buildRetryTemplate(properties));
			adapter.setRecoveryCallback(errorInfrastructure.getRecoverer());
		}
		else {
			adapter.setErrorMessageStrategy(errorMessageStrategy);
			adapter.setErrorChannel(errorInfrastructure.getErrorChannel());
		}
		adapter.setMessageConverter(passThoughConverter);
		return adapter;
		*/
	}

	@Override
	protected PolledConsumerResources createPolledConsumerResources(String name,
			String group, ConsumerDestination destination,
			ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties) {

		NatsMessageSource source = new NatsMessageSource(destination.getName(), group, consumerProperties);
		return new PolledConsumerResources(source, registerErrorInfrastructure(
				destination, group, consumerProperties, true));
	}

	@Override
	protected void postProcessPollableSource(DefaultPollableMessageSource bindingTarget) {
		// TODO Relevant for NATS?  Rabbit just set headers.
	}

	@Override
	protected ErrorMessageStrategy getErrorMessageStrategy() {
		return null;  // FIXME...  super.errorMessageStrategy;
	}

	@Override
	protected MessageHandler getErrorMessageHandler(ConsumerDestination destination,
			String group,
			final ExtendedConsumerProperties<NatsConsumerProperties> properties) {

		// See if the default suffices...

		// return new MessageHandler() {
		// 	@Override
		// 	public void handleMessage(
		// 			org.springframework.messaging.Message<?> message)
		// 			throws MessagingException {
		// 		if (!(message instanceof ErrorMessage)) {
		// 			logger.error("Expected an ErrorMessage, not a "
		// 					+ message.getClass().toString() + " for: " + message);
		// 		}
		// 		else {
		// 			logger.debug("Core NATS does not republish messages.  Providing default behavior.");
		// 		}
		// 	}
		// }

		return super.getErrorMessageHandler(destination, group, properties);
	}

	@Override
	protected MessageHandler getPolledConsumerErrorMessageHandler(
			ConsumerDestination destination, String group,
			ExtendedConsumerProperties<NatsConsumerProperties> properties) {

		// TODO:  required?
		return super.getErrorMessageHandler(destination, group, properties);
	}

	@Override
	protected String errorsBaseName(ConsumerDestination destination, String group,
			ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties) {
		return destination.getName() + ".errors";
	}

	@Override
	protected void afterUnbindConsumer(ConsumerDestination consumerDestination,
			String group,
			ExtendedConsumerProperties<NatsConsumerProperties> consumerProperties) {
		provisioningProvider.cleanAutoDeclareContext(consumerDestination,
				consumerProperties);
	}

	// Not sure a converter is needed, bytes <->> object is supposed to be
	// natitvely supported.
	// private static final class SimplePassthroughMessageConverter
	// 		extends AbstractMessageConverter {

	// 	SimplePassthroughMessageConverter() {
	// 		super();
	// 	}

	// 	@Override
	// 	protected Message createMessage(Object object,
	// 			MessageProperties messageProperties) {
	// 		if (object instanceof byte[]) {
	// 			return new Message((byte[]) object, messageProperties);
	// 		}
	// 		else {
	// 			// just for safety (backwards compatibility)
	// 			return new Message(object, messageProperties);
	// 		}
	// 	}

	// 	@Override
	// 	public Object fromMessage(Message message) throws MessageConversionException {
	// 		return message.getBody();
	// 	}
	// }

	class NatsMessageChannel implements MessageChannel {

		String subject;

		// TODO - pass/get connection, etc.
		NatsMessageChannel(Connection connection, String subject) {
			this.subject = subject;
		}

		@Override
		public boolean send(Message<?> message) {
			// TODO: send -  flush too?
			return false;
		}

		@Override
		public boolean send(Message<?> message, long timeout) {
			// TODO: send - flush too?
			return false;
		}
	}

	// TODO:  Not sure we need this or if we can use a built
	// in one.
	class NatsInboundMessageProducer implements MessageProducer {
		MessageChannel channel;

		NatsInboundMessageProducer(Connection connection, String subject) {
			channel = new NatsMessageChannel(connection, subject);
		}

		@Override
		public MessageChannel getOutputChannel() {
			return channel;
		}

		@Override
		public void setOutputChannel(MessageChannel outputChannel) {
			channel = outputChannel;
		}

		@Override
		public void setOutputChannelName(String outputChannel) {
			//channel.setName(outputChannel);
		}
	}
}
