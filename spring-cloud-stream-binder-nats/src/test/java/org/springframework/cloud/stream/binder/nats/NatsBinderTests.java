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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.PartitionCapableBinderTests;
import org.springframework.cloud.stream.binder.PartitionSelectorStrategy;
import org.springframework.cloud.stream.binder.Spy;
import org.springframework.cloud.stream.binder.nats.properties.NatsConsumerProperties;
import org.springframework.cloud.stream.binder.nats.properties.NatsProducerProperties;
import org.springframework.context.Lifecycle;

import static org.assertj.core.api.Assertions.assertThat;


// For now, just in case we'll need them for tests.
//
// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.atomic.AtomicReference;
// import org.springframework.cloud.stream.binder.Binding;
// import org.springframework.cloud.stream.config.BindingProperties;
// import org.springframework.context.ApplicationListener;
// import org.springframework.integration.channel.DirectChannel;
// import org.springframework.integration.support.MessageBuilder;
// import org.springframework.messaging.Message;
// import org.springframework.messaging.MessageChannel;
// import org.springframework.messaging.MessageHandler;
// import org.springframework.messaging.MessageHeaders;
// import org.springframework.messaging.MessagingException;
// import org.springframework.messaging.support.ErrorMessage;
// import org.springframework.integration.context.IntegrationContextUtils;
// import org.springframework.util.ReflectionUtils;
//import org.springframework.messaging.SubscribableChannel;
// import org.springframework.cloud.stream.binder.DefaultPollableMessageSource;
// import java.util.List;
// import java.lang.reflect.Constructor;
// import org.springframework.cloud.stream.binder.PartitionTestSupport;
// import org.springframework.cloud.stream.binder.PollableSource;
// import org.springframework.context.ApplicationContext;
// import org.springframework.expression.spel.standard.SpelExpression;
// import org.springframework.integration.channel.QueueChannel;
// import org.springframework.messaging.support.GenericMessage;
// import java.util.Arrays;
// import java.util.Map;
// import java.util.concurrent.atomic.AtomicBoolean;
//import org.springframework.cloud.stream.binder.test.junit.nats.NatsTestSupport;
// import org.springframework.cloud.stream.binder.nats.provisioning.NatsSubjectProvisioner;
// import static org.assertj.core.api.Assertions.fail;
// import org.springframework.messaging.support.ChannelInterceptor;
// import org.springframework.context.ConfigurableApplicationContext;
// import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
// import org.springframework.cloud.stream.binder.BinderException;
// import org.springframework.cloud.stream.binder.PartitionKeyExtractorStrategy;
// import java.nio.charset.StandardCharsets;
// import org.apache.commons.logging.Log;
// import java.util.zip.Deflater;
// import org.mockito.ArgumentCaptor;
// import org.springframework.beans.DirectFieldAccessor;
// import org.springframework.util.MimeTypeUtils;
// import static org.mockito.Mockito.spy;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;
//import org.springframework.messaging.MessageHandlingException;
//import org.springframework.cloud.stream.binder.RequeueCurrentMessageException;
//import org.springframework.integration.expression.ValueExpression;

/**
 * @author The NATS Authors
 */
// @checkstyle:off
public class NatsBinderTests extends
		PartitionCapableBinderTests<NatsTestBinder, ExtendedConsumerProperties<NatsConsumerProperties>, ExtendedProducerProperties<NatsProducerProperties>> {

	private final String CLASS_UNDER_TEST_NAME = NatsMessageChannelBinder.class
			.getSimpleName();

	public static final String TEST_PREFIX = "bindertest.";

	private static final String BIG_EXCEPTION_MESSAGE = new String(new byte[10_000])
			.replaceAll("\u0000", "x");

	private int maxStackTraceSize;

	@Rule
	public TestName testName = new TestName();

	@Override
	protected NatsTestBinder getBinder() {
		if (this.testBinder == null) {
			this.testBinder = new NatsTestBinder();
		}
		return this.testBinder;
	}

	@Override
	protected boolean usesExplicitRouting() {
		return false;
	}
	
	@Override
	protected ExtendedConsumerProperties<NatsConsumerProperties> createConsumerProperties() {
		return new ExtendedConsumerProperties<>(new NatsConsumerProperties());
	}

	@Override
	protected ExtendedProducerProperties<NatsProducerProperties> createProducerProperties() {
		ExtendedProducerProperties<NatsProducerProperties> props = new ExtendedProducerProperties<>(
				new NatsProducerProperties());
		if (testName.getMethodName().equals("testPartitionedModuleSpEL")) {
			props.getExtension().setRoutingKeyExpression(
					spelExpressionParser.parseExpression("'part.0'"));
		}
		return props;
	}

	@Test
	public void testSendAndReceiveBad() throws Exception {
		// NatsTestBinder binder = getBinder();
		// final AtomicReference<AsyncConsumerStartedEvent> event = new AtomicReference<>();
		// binder.getApplicationContext().addApplicationListener(
		// 		(ApplicationListener<AsyncConsumerStartedEvent>) e -> event.set(e));
		// DirectChannel moduleOutputChannel = createBindableChannel("output",
		// 		new BindingProperties());
		// DirectChannel moduleInputChannel = createBindableChannel("input",
		// 		new BindingProperties());
		// Binding<MessageChannel> producerBinding = binder.bindProducer("bad.0",
		// 		moduleOutputChannel, createProducerProperties());
		// assertThat(TestUtils.getPropertyValue(producerBinding,
		// 		"lifecycle.headersMappedLast", Boolean.class)).isTrue();
		// ExtendedConsumerProperties<NatsConsumerProperties> consumerProps = createConsumerProperties();
		// consumerProps.getExtension().setContainerType(ContainerType.DIRECT);
		// Binding<MessageChannel> consumerBinding = binder.bindConsumer("bad.0", "test",
		// 		moduleInputChannel, consumerProps);
		// assertThat(
		// 		TestUtils.getPropertyValue(consumerBinding, "lifecycle.messageConverter")
		// 				.getClass().getName()).contains("Passthrough");
		// assertThat(TestUtils.getPropertyValue(consumerBinding,
		// 		"lifecycle.messageListenerContainer"))
		// 				.isInstanceOf(DirectMessageListenerContainer.class);
		// Message<?> message = MessageBuilder.withPayload("bad".getBytes())
		// 		.setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		// final CountDownLatch latch = new CountDownLatch(3);
		// moduleInputChannel.subscribe(new MessageHandler() {

		// 	@Override
		// 	public void handleMessage(Message<?> message) throws MessagingException {
		// 		latch.countDown();
		// 		throw new RuntimeException("bad");
		// 	}
		// });
		// moduleOutputChannel.send(message);
		// assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		// assertThat(event.get()).isNotNull();
		// producerBinding.unbind();
		// consumerBinding.unbind();
	}

	@Test
	public void testProducerErrorChannel() throws Exception {
		// NatsTestBinder binder = getBinder();
		// CachingConnectionFactory ccf = this.NatsAvailableRule.getResource();
		// ccf.setPublisherReturns(true);
		// ccf.setPublisherConfirms(true);
		// ccf.resetConnection();
		// DirectChannel moduleOutputChannel = createBindableChannel("output",
		// 		new BindingProperties());
		// ExtendedProducerProperties<NatsProducerProperties> producerProps = createProducerProperties();
		// producerProps.setErrorChannelEnabled(true);
		// Binding<MessageChannel> producerBinding = binder.bindProducer("ec.0",
		// 		moduleOutputChannel, producerProps);
		// final Message<?> message = MessageBuilder.withPayload("bad".getBytes())
		// 		.setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		// SubscribableChannel ec = binder.getApplicationContext().getBean("ec.0.errors",
		// 		SubscribableChannel.class);
		// final AtomicReference<Message<?>> errorMessage = new AtomicReference<>();
		// final CountDownLatch latch = new CountDownLatch(2);
		// ec.subscribe(new MessageHandler() {

		// 	@Override
		// 	public void handleMessage(Message<?> message) throws MessagingException {
		// 		errorMessage.set(message);
		// 		latch.countDown();
		// 	}

		// });
		// SubscribableChannel globalEc = binder.getApplicationContext().getBean(
		// 		IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME,
		// 		SubscribableChannel.class);
		// globalEc.subscribe(new MessageHandler() {

		// 	@Override
		// 	public void handleMessage(Message<?> message) throws MessagingException {
		// 		latch.countDown();
		// 	}

		// });
		// moduleOutputChannel.send(message);
		// assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		// assertThat(errorMessage.get()).isInstanceOf(ErrorMessage.class);
		// assertThat(errorMessage.get().getPayload())
		// 		.isInstanceOf(ReturnedAmqpMessageException.class);
		// ReturnedAmqpMessageException exception = (ReturnedAmqpMessageException) errorMessage
		// 		.get().getPayload();
		// assertThat(exception.getReplyCode()).isEqualTo(312);
		// assertThat(exception.getReplyText()).isEqualTo("NO_ROUTE");

		// AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(producerBinding,
		// 		"lifecycle", AmqpOutboundEndpoint.class);
		// assertThat(TestUtils.getPropertyValue(endpoint,
		// 		"confirmCorrelationExpression.expression")).isEqualTo("#root");
		// class WrapperAccessor extends AmqpOutboundEndpoint {

		// 	WrapperAccessor(AmqpTemplate amqpTemplate) {
		// 		super(amqpTemplate);
		// 	}

		// 	CorrelationDataWrapper getWrapper() throws Exception {
		// 		Constructor<CorrelationDataWrapper> constructor = CorrelationDataWrapper.class
		// 				.getDeclaredConstructor(String.class, Object.class,
		// 						Message.class);
		// 		ReflectionUtils.makeAccessible(constructor);
		// 		return constructor.newInstance(null, message, message);
		// 	}

		// }
		// endpoint.confirm(new WrapperAccessor(mock(AmqpTemplate.class)).getWrapper(),
		// 		false, "Mock NACK");
		// assertThat(errorMessage.get()).isInstanceOf(ErrorMessage.class);
		// assertThat(errorMessage.get().getPayload())
		// 		.isInstanceOf(NackedAmqpMessageException.class);
		// NackedAmqpMessageException nack = (NackedAmqpMessageException) errorMessage.get()
		// 		.getPayload();
		// assertThat(nack.getNackReason()).isEqualTo("Mock NACK");
		// assertThat(nack.getCorrelationData()).isEqualTo(message);
		// assertThat(nack.getFailedMessage()).isEqualTo(message);
		// producerBinding.unbind();
	}

	@Test
	public void testProducerAckChannel() throws Exception {
		// NatsTestBinder binder = getBinder();
		// DirectChannel moduleOutputChannel = createBindableChannel("output",
		// 		new BindingProperties());
		// ExtendedProducerProperties<NatsProducerProperties> producerProps = createProducerProperties();
		// producerProps.setErrorChannelEnabled(true);
		// producerProps.getExtension().setConfirmAckChannel("acksChannel");
		// Binding<MessageChannel> producerBinding = binder.bindProducer("acks.0",
		// 		moduleOutputChannel, producerProps);
		// final Message<?> message = MessageBuilder.withPayload("acksMessage".getBytes())
		// 		.build();
		// final AtomicReference<Message<?>> confirm = new AtomicReference<>();
		// final CountDownLatch confirmLatch = new CountDownLatch(1);
		// binder.getApplicationContext().getBean("acksChannel", DirectChannel.class)
		// 		.subscribe(m -> {
		// 			confirm.set(m);
		// 			confirmLatch.countDown();
		// 		});
		// moduleOutputChannel.send(message);
		// assertThat(confirmLatch.await(10, TimeUnit.SECONDS)).isTrue();
		// assertThat(confirm.get().getPayload()).isEqualTo("acksMessage".getBytes());
		// producerBinding.unbind();
	}

	@Test
	public void testConsumerProperties() throws Exception {
		// NatsTestBinder binder = getBinder();
		// ExtendedConsumerProperties<NatsConsumerProperties> properties = createConsumerProperties();
		// properties.getExtension().setRequeueRejected(true);
		// properties.getExtension().setTransacted(true);
		// properties.getExtension().setExclusive(true);
		// properties.getExtension().setMissingQueuesFatal(true);
		// properties.getExtension().setFailedDeclarationRetryInterval(1500L);
		// properties.getExtension().setQueueDeclarationRetries(23);
		// Binding<MessageChannel> consumerBinding = binder.bindConsumer("props.0", null,
		// 		createBindableChannel("input", new BindingProperties()), properties);
		// Lifecycle endpoint = extractEndpoint(consumerBinding);
		// SimpleMessageListenerContainer container = TestUtils.getPropertyValue(endpoint,
		// 		"messageListenerContainer", SimpleMessageListenerContainer.class);
		// assertThat(container.getAcknowledgeMode()).isEqualTo(AcknowledgeMode.AUTO);
		// assertThat(container.getQueueNames()[0])
		// 		.startsWith(properties.getExtension().getPrefix());
		// assertThat(TestUtils.getPropertyValue(container, "transactional", Boolean.class))
		// 		.isTrue();
		// assertThat(TestUtils.getPropertyValue(container, "exclusive", Boolean.class))
		// 		.isTrue();
		// assertThat(TestUtils.getPropertyValue(container, "concurrentConsumers"))
		// 		.isEqualTo(1);
		// assertThat(TestUtils.getPropertyValue(container, "maxConcurrentConsumers"))
		// 		.isNull();
		// assertThat(TestUtils.getPropertyValue(container, "defaultRequeueRejected",
		// 		Boolean.class)).isTrue();
		// assertThat(TestUtils.getPropertyValue(container, "prefetchCount")).isEqualTo(1);
		// assertThat(TestUtils.getPropertyValue(container, "txSize")).isEqualTo(1);
		// assertThat(TestUtils.getPropertyValue(container, "missingQueuesFatal",
		// 		Boolean.class)).isTrue();
		// assertThat(
		// 		TestUtils.getPropertyValue(container, "failedDeclarationRetryInterval"))
		// 				.isEqualTo(1500L);
		// assertThat(TestUtils.getPropertyValue(container, "declarationRetries"))
		// 		.isEqualTo(23);
		// RetryTemplate retry = TestUtils.getPropertyValue(endpoint, "retryTemplate",
		// 		RetryTemplate.class);
		// assertThat(TestUtils.getPropertyValue(retry, "retryPolicy.maxAttempts"))
		// 		.isEqualTo(3);
		// assertThat(TestUtils.getPropertyValue(retry, "backOffPolicy.initialInterval"))
		// 		.isEqualTo(1000L);
		// assertThat(TestUtils.getPropertyValue(retry, "backOffPolicy.maxInterval"))
		// 		.isEqualTo(10000L);
		// assertThat(TestUtils.getPropertyValue(retry, "backOffPolicy.multiplier"))
		// 		.isEqualTo(2.0);
		// consumerBinding.unbind();
		// assertThat(endpoint.isRunning()).isFalse();

		// properties = createConsumerProperties();
		// properties.getExtension().setAcknowledgeMode(AcknowledgeMode.NONE);
		// properties.setBackOffInitialInterval(2000);
		// properties.setBackOffMaxInterval(20000);
		// properties.setBackOffMultiplier(5.0);
		// properties.setConcurrency(2);
		// properties.setMaxAttempts(23);
		// properties.getExtension().setMaxConcurrency(3);
		// properties.getExtension().setPrefix("foo.");
		// properties.getExtension().setPrefetch(20);
		// properties.getExtension().setHeaderPatterns(new String[] { "foo" });
		// properties.getExtension().setTxSize(10);
		// properties.setInstanceIndex(0);
		// consumerBinding = binder.bindConsumer("props.0", "test",
		// 		createBindableChannel("input", new BindingProperties()), properties);

		// endpoint = extractEndpoint(consumerBinding);
		// container = verifyContainer(endpoint);

		// assertThat(container.getQueueNames()[0]).isEqualTo("foo.props.0.test");

		// consumerBinding.unbind();
		// assertThat(endpoint.isRunning()).isFalse();
	}

	@Test
	public void testConsumerPropertiesWithUserInfrastructureNoBind() throws Exception {
		// NatsAdmin admin = new NatsAdmin(this.NatsAvailableRule.getResource());
		// Queue queue = new Queue("propsUser1.infra");
		// admin.declareQueue(queue);
		// DirectExchange exchange = new DirectExchange("propsUser1");
		// admin.declareExchange(exchange);
		// admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("foo"));

		// NatsTestBinder binder = getBinder();
		// ExtendedConsumerProperties<NatsConsumerProperties> properties = createConsumerProperties();
		// properties.getExtension().setDeclareExchange(false);
		// properties.getExtension().setBindQueue(false);

		// Binding<MessageChannel> consumerBinding = binder.bindConsumer("propsUser1",
		// 		"infra", createBindableChannel("input", new BindingProperties()),
		// 		properties);
		// Lifecycle endpoint = extractEndpoint(consumerBinding);
		// SimpleMessageListenerContainer container = TestUtils.getPropertyValue(endpoint,
		// 		"messageListenerContainer", SimpleMessageListenerContainer.class);
		// assertThat(TestUtils.getPropertyValue(container, "missingQueuesFatal",
		// 		Boolean.class)).isFalse();
		// assertThat(container.isRunning()).isTrue();
		// consumerBinding.unbind();
		// assertThat(container.isRunning()).isFalse();
		// Client client = new Client("http://guest:guest@localhost:15672/api/");
		// List<?> bindings = client.getBindingsBySource("/", exchange.getName());
		// assertThat(bindings.size()).isEqualTo(1);
	}

	@Test
	public void testAnonWithBuiltInExchange() throws Exception {
		// NatsTestBinder binder = getBinder();
		// ExtendedConsumerProperties<NatsConsumerProperties> properties = createConsumerProperties();
		// properties.getExtension().setDeclareExchange(false);
		// properties.getExtension().setQueueNameGroupOnly(true);

		// Binding<MessageChannel> consumerBinding = binder.bindConsumer("amq.topic", null,
		// 		createBindableChannel("input", new BindingProperties()), properties);
		// Lifecycle endpoint = extractEndpoint(consumerBinding);
		// SimpleMessageListenerContainer container = TestUtils.getPropertyValue(endpoint,
		// 		"messageListenerContainer", SimpleMessageListenerContainer.class);
		// String queueName = container.getQueueNames()[0];
		// assertThat(queueName).startsWith("anonymous.");
		// assertThat(container.isRunning()).isTrue();
		// consumerBinding.unbind();
		// assertThat(container.isRunning()).isFalse();
	}

	@Test
	public void testAnonWithBuiltInExchangeCustomPrefix() throws Exception {
		// NatsTestBinder binder = getBinder();
		// ExtendedConsumerProperties<NatsConsumerProperties> properties = createConsumerProperties();
		// properties.getExtension().setDeclareExchange(false);
		// properties.getExtension().setQueueNameGroupOnly(true);
		// properties.getExtension().setAnonymousGroupPrefix("customPrefix.");

		// Binding<MessageChannel> consumerBinding = binder.bindConsumer("topic", null,
		// 		createBindableChannel("input", new BindingProperties()), properties);
		// Lifecycle endpoint = extractEndpoint(consumerBinding);
		// SimpleMessageListenerContainer container = TestUtils.getPropertyValue(endpoint,
		// 		"messageListenerContainer", SimpleMessageListenerContainer.class);
		// String queueName = container.getQueueNames()[0];
		// assertThat(queueName).startsWith("customPrefix.");
		// assertThat(container.isRunning()).isTrue();
		// consumerBinding.unbind();
		// assertThat(container.isRunning()).isFalse();
	}

	@Test
	public void testConsumerPropertiesWithUserInfrastructureCustomExchangeAndRK()
			throws Exception {
		// NatsTestBinder binder = getBinder();
		// ExtendedConsumerProperties<NatsConsumerProperties> properties = createConsumerProperties();
		// properties.getExtension().setExchangeType(ExchangeTypes.DIRECT);
		// properties.getExtension().setBindingRoutingKey("foo");
		// properties.getExtension().setQueueNameGroupOnly(true);
		// // properties.getExtension().setDelayedExchange(true); // requires delayed message
		// // exchange plugin; tested locally

		// String group = "infra";
		// Binding<MessageChannel> consumerBinding = binder.bindConsumer("propsUser2", group,
		// 		createBindableChannel("input", new BindingProperties()), properties);
		// Lifecycle endpoint = extractEndpoint(consumerBinding);
		// SimpleMessageListenerContainer container = TestUtils.getPropertyValue(endpoint,
		// 		"messageListenerContainer", SimpleMessageListenerContainer.class);
		// assertThat(container.isRunning()).isTrue();
		// consumerBinding.unbind();
		// assertThat(container.isRunning()).isFalse();
		// assertThat(container.getQueueNames()[0]).isEqualTo(group);
		// Client client = new Client("http://guest:guest@localhost:15672/api/");
		// List<BindingInfo> bindings = client.getBindingsBySource("/", "propsUser2");
		// int n = 0;
		// while (n++ < 100 && bindings == null || bindings.size() < 1) {
		// 	Thread.sleep(100);
		// 	bindings = client.getBindingsBySource("/", "propsUser2");
		// }
		// assertThat(bindings.size()).isEqualTo(1);
		// assertThat(bindings.get(0).getSource()).isEqualTo("propsUser2");
		// assertThat(bindings.get(0).getDestination()).isEqualTo(group);
		// assertThat(bindings.get(0).getRoutingKey()).isEqualTo("foo");

		// ExchangeInfo exchange = client.getExchange("/", "propsUser2");
		// while (n++ < 100 && exchange == null) {
		// 	Thread.sleep(100);
		// 	exchange = client.getExchange("/", "propsUser2");
		// }
		// assertThat(exchange.getType()).isEqualTo("direct");
		// assertThat(exchange.isDurable()).isEqualTo(true);
		// assertThat(exchange.isAutoDelete()).isEqualTo(false);
	}

	@Test
	public void testConsumerPropertiesWithUserInfrastructureCustomQueueArgs()
			throws Exception {
		// NatsTestBinder binder = getBinder();
		// ExtendedConsumerProperties<NatsConsumerProperties> properties = createConsumerProperties();
		// NatsConsumerProperties extProps = properties.getExtension();
		// extProps.setExchangeType(ExchangeTypes.DIRECT);
		// extProps.setExchangeDurable(false);
		// extProps.setExchangeAutoDelete(true);
		// extProps.setBindingRoutingKey("foo");
		// extProps.setExpires(30_000);
		// extProps.setLazy(true);
		// extProps.setMaxLength(10_000);
		// extProps.setMaxLengthBytes(100_000);
		// extProps.setMaxPriority(10);
		// extProps.setOverflowBehavior("drop-head");
		// extProps.setTtl(2_000);
		// extProps.setAutoBindDlq(true);
		// extProps.setDeadLetterQueueName("customDLQ");
		// extProps.setDeadLetterExchange("customDLX");
		// extProps.setDeadLetterExchangeType(ExchangeTypes.TOPIC);
		// extProps.setDeadLetterRoutingKey("customDLRK");
		// extProps.setDlqDeadLetterExchange("propsUser3");
		// extProps.setDlqDeadLetterRoutingKey("propsUser3");
		// extProps.setDlqExpires(60_000);
		// extProps.setDlqLazy(true);
		// extProps.setDlqMaxLength(20_000);
		// extProps.setDlqMaxLengthBytes(40_000);
		// extProps.setDlqOverflowBehavior("reject-publish");
		// extProps.setDlqMaxPriority(8);
		// extProps.setDlqTtl(1_000);
		// extProps.setConsumerTagPrefix("testConsumerTag");
		// extProps.setExclusive(true);

		// Binding<MessageChannel> consumerBinding = binder.bindConsumer("propsUser3",
		// 		"infra", createBindableChannel("input", new BindingProperties()),
		// 		properties);
		// Lifecycle endpoint = extractEndpoint(consumerBinding);
		// SimpleMessageListenerContainer container = TestUtils.getPropertyValue(endpoint,
		// 		"messageListenerContainer", SimpleMessageListenerContainer.class);
		// assertThat(container.isRunning()).isTrue();
		// Client client = new Client("http://guest:guest@localhost:15672/api");
		// List<BindingInfo> bindings = client.getBindingsBySource("/", "propsUser3");
		// int n = 0;
		// while (n++ < 100 && bindings == null || bindings.size() < 1) {
		// 	Thread.sleep(100);
		// 	bindings = client.getBindingsBySource("/", "propsUser3");
		// }
		// assertThat(bindings.size()).isEqualTo(1);
		// assertThat(bindings.get(0).getSource()).isEqualTo("propsUser3");
		// assertThat(bindings.get(0).getDestination()).isEqualTo("propsUser3.infra");
		// assertThat(bindings.get(0).getRoutingKey()).isEqualTo("foo");

		// ExchangeInfo exchange = client.getExchange("/", "propsUser3");
		// n = 0;
		// while (n++ < 100 && exchange == null) {
		// 	Thread.sleep(100);
		// 	exchange = client.getExchange("/", "propsUser3");
		// }
		// assertThat(exchange.getType()).isEqualTo("direct");
		// assertThat(exchange.isDurable()).isEqualTo(false);
		// assertThat(exchange.isAutoDelete()).isEqualTo(true);

		// exchange = client.getExchange("/", "customDLX");
		// n = 0;
		// while (n++ < 100 && exchange == null) {
		// 	Thread.sleep(100);
		// 	exchange = client.getExchange("/", "customDLX");
		// }
		// assertThat(exchange.getType()).isEqualTo("topic");
		// assertThat(exchange.isDurable()).isEqualTo(true);
		// assertThat(exchange.isAutoDelete()).isEqualTo(false);

		// QueueInfo queue = client.getQueue("/", "propsUser3.infra");
		// n = 0;
		// while (n++ < 100 && queue == null || queue.getConsumerCount() == 0) {
		// 	Thread.sleep(100);
		// 	queue = client.getQueue("/", "propsUser3.infra");
		// }
		// assertThat(queue).isNotNull();
		// Map<String, Object> args = queue.getArguments();
		// assertThat(args.get("x-expires")).isEqualTo(30_000);
		// assertThat(args.get("x-max-length")).isEqualTo(10_000);
		// assertThat(args.get("x-max-length-bytes")).isEqualTo(100_000);
		// assertThat(args.get("x-overflow")).isEqualTo("drop-head");
		// assertThat(args.get("x-max-priority")).isEqualTo(10);
		// assertThat(args.get("x-message-ttl")).isEqualTo(2_000);
		// assertThat(args.get("x-dead-letter-exchange")).isEqualTo("customDLX");
		// assertThat(args.get("x-dead-letter-routing-key")).isEqualTo("customDLRK");
		// assertThat(args.get("x-queue-mode")).isEqualTo("lazy");
		// assertThat(queue.getExclusiveConsumerTag()).isEqualTo("testConsumerTag#0");

		// queue = client.getQueue("/", "customDLQ");

		// n = 0;
		// while (n++ < 100 && queue == null) {
		// 	Thread.sleep(100);
		// 	queue = client.getQueue("/", "customDLQ");
		// }
		// assertThat(queue).isNotNull();
		// args = queue.getArguments();
		// assertThat(args.get("x-expires")).isEqualTo(60_000);
		// assertThat(args.get("x-max-length")).isEqualTo(20_000);
		// assertThat(args.get("x-max-length-bytes")).isEqualTo(40_000);
		// assertThat(args.get("x-overflow")).isEqualTo("reject-publish");
		// assertThat(args.get("x-max-priority")).isEqualTo(8);
		// assertThat(args.get("x-message-ttl")).isEqualTo(1_000);
		// assertThat(args.get("x-dead-letter-exchange")).isEqualTo("propsUser3");
		// assertThat(args.get("x-dead-letter-routing-key")).isEqualTo("propsUser3");
		// assertThat(args.get("x-queue-mode")).isEqualTo("lazy");

		// consumerBinding.unbind();
		// assertThat(container.isRunning()).isFalse();
	}


	/*
	 * Test late binding due to the NATS server down.
	 */
	@Test
	public void testLateBinding() throws Exception {

		// NatsMessageChannelBinder NatsBinder = new NatsMessageChannelBinder(cf,
		// 		new NatsProperties(), new NatsExchangeQueueProvisioner(cf));
		// NatsTestBinder binder = new NatsTestBinder(cf, NatsBinder);

		// ExtendedProducerProperties<NatsProducerProperties> producerProperties = createProducerProperties();
		// producerProperties.getExtension().setPrefix("latebinder.");

		// MessageChannel moduleOutputChannel = createBindableChannel("output",
		// 		createProducerBindingProperties(producerProperties));
		// Binding<MessageChannel> late0ProducerBinding = binder.bindProducer("late.0",
		// 		moduleOutputChannel, producerProperties);

		// QueueChannel moduleInputChannel = new QueueChannel();
		// ExtendedConsumerProperties<NatsConsumerProperties> NatsConsumerProperties = createConsumerProperties();
		// NatsConsumerProperties.getExtension().setPrefix("latebinder.");
		// Binding<MessageChannel> late0ConsumerBinding = binder.bindConsumer("late.0",
		// 		"test", moduleInputChannel, NatsConsumerProperties);

		// QueueChannel partInputChannel0 = new QueueChannel();
		// QueueChannel partInputChannel1 = new QueueChannel();

		// ExtendedConsumerProperties<NatsConsumerProperties> partLateConsumerProperties = createConsumerProperties();
		// partLateConsumerProperties.getExtension().setPrefix("latebinder.");
		// Binding<MessageChannel> partlate0Consumer0Binding = binder.bindConsumer(
		// 		"partlate.0", "test", partInputChannel0, partLateConsumerProperties);
		// partLateConsumerProperties.setInstanceIndex(1);
		// Binding<MessageChannel> partlate0Consumer1Binding = binder.bindConsumer(
		// 		"partlate.0", "test", partInputChannel1, partLateConsumerProperties);

		// MessageChannel outputChannel = createBindableChannel("output",
		// 		createProducerBindingProperties(noDlqProducerProperties));
		// Binding<MessageChannel> pubSubProducerBinding = binder.bindProducer("latePubSub",
		// 		outputChannel, noDlqProducerProperties);
		// QueueChannel pubSubInputChannel = new QueueChannel();
		// noDlqConsumerProperties.getExtension().setDurableSubscription(false);
		// Binding<MessageChannel> nonDurableConsumerBinding = binder.bindConsumer(
		// 		"latePubSub", "lategroup", pubSubInputChannel, noDlqConsumerProperties);
		// QueueChannel durablePubSubInputChannel = new QueueChannel();
		// noDlqConsumerProperties.getExtension().setDurableSubscription(true);
		// Binding<MessageChannel> durableConsumerBinding = binder.bindConsumer("latePubSub",
		// 		"lateDurableGroup", durablePubSubInputChannel, noDlqConsumerProperties);

		// proxy.start();

		// moduleOutputChannel.send(MessageBuilder.withPayload("foo")
		// 		.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
		// 		.build());
		// Message<?> message = moduleInputChannel.receive(10000);
		// assertThat(message).isNotNull();
		// assertThat(message.getPayload()).isNotNull();

		// noDLQOutputChannel.send(MessageBuilder.withPayload("bar")
		// 		.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
		// 		.build());
		// message = noDLQInputChannel.receive(10000);
		// assertThat(message);
		// assertThat(message.getPayload()).isEqualTo("bar".getBytes());

		// outputChannel.send(MessageBuilder.withPayload("baz")
		// 		.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
		// 		.build());
		// message = pubSubInputChannel.receive(10000);
		// assertThat(message);
		// assertThat(message.getPayload()).isEqualTo("baz".getBytes());
		// message = durablePubSubInputChannel.receive(10000);
		// assertThat(message).isNotNull();
		// assertThat(message.getPayload()).isEqualTo("baz".getBytes());

		// partOutputChannel.send(MessageBuilder.withPayload("0")
		// 		.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
		// 		.build());
		// partOutputChannel.send(MessageBuilder.withPayload("1")
		// 		.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN)
		// 		.build());
		// message = partInputChannel0.receive(10000);
		// assertThat(message).isNotNull();
		// assertThat(message.getPayload()).isEqualTo("0".getBytes());
		// message = partInputChannel1.receive(10000);
		// assertThat(message).isNotNull();
		// assertThat(message.getPayload()).isEqualTo("1".getBytes());

		// late0ProducerBinding.unbind();
		// late0ConsumerBinding.unbind();
		// partlate0ProducerBinding.unbind();
		// partlate0Consumer0Binding.unbind();
		// partlate0Consumer1Binding.unbind();
		// noDlqProducerBinding.unbind();
		// noDlqConsumerBinding.unbind();
		// pubSubProducerBinding.unbind();
		// nonDurableConsumerBinding.unbind();
		// durableConsumerBinding.unbind();

		// binder.cleanup();

		// proxy.stop();
		// cf.destroy();

		// this.NatsAvailableRule.getResource().destroy();
	}

	@Test
	public void testPolledConsumer() throws Exception {
		// NatsTestBinder binder = getBinder();
		// PollableSource<MessageHandler> inboundBindTarget = new DefaultPollableMessageSource(
		// 		this.messageConverter);
		// Binding<PollableSource<MessageHandler>> binding = binder.bindPollableConsumer(
		// 		"pollable", "group", inboundBindTarget, createConsumerProperties());
		// NatsTemplate template = new NatsTemplate(
		// 		this.NatsAvailableRule.getResource());
		// template.convertAndSend("pollable.group", "testPollable");
		// boolean polled = inboundBindTarget.poll(m -> {
		// 	assertThat(m.getPayload()).isEqualTo("testPollable");
		// });
		// int n = 0;
		// while (n++ < 100 && !polled) {
		// 	polled = inboundBindTarget.poll(m -> {
		// 		assertThat(m.getPayload()).isEqualTo("testPollable");
		// 	});
		// }
		// assertThat(polled).isTrue();
		// binding.unbind();
	}

	private void verifyFooRequestProducer(Lifecycle endpoint) {
		// List<?> requestMatchers = TestUtils.getPropertyValue(endpoint,
		// 		"headerMapper.requestHeaderMatcher.matchers", List.class);
		// assertThat(requestMatchers).hasSize(2);
		// assertThat(TestUtils.getPropertyValue(requestMatchers.get(1), "pattern"))
		// 		.isEqualTo("foo");
	}

	@Override
	protected String getEndpointRouting(Object endpoint) {
		// TODO
		return "foo";
	}

	@Override
	protected String getExpectedRoutingBaseDestination(String name, String group) {
		return name;
	}

	@Override
	protected String getClassUnderTestName() {
		return CLASS_UNDER_TEST_NAME;
	}

	@Override
	protected void checkRkExpressionForPartitionedModuleSpEL(Object endpoint) {
		assertThat(getEndpointRouting(endpoint))
				.contains(getExpectedRoutingBaseDestination("'part.0'", "test")
						+ " + '-' + headers['" + BinderHeaders.PARTITION_HEADER + "']");
	}

	@Override
	public Spy spyOn(final String queue) {
		return null;
	}

	private RuntimeException bigCause(RuntimeException cause) {
		if (getStackTraceAsString(cause).length() > this.maxStackTraceSize) {
			return cause;
		}
		return bigCause(new RuntimeException(BIG_EXCEPTION_MESSAGE, cause));
	}

	private String getStackTraceAsString(Throwable cause) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter, true);
		cause.printStackTrace(printWriter);
		return stringWriter.getBuffer().toString();
	}

	public static class TestPartitionSelectorClass implements PartitionSelectorStrategy {

		@Override
		public int selectPartition(Object key, int partitionCount) {
			return 0;
		}

	}

	public static class Pojo {

		private String field;

		public Pojo() {
			super();
		}

		public Pojo(String field) {
			this.field = field;
		}

		public String getField() {
			return this.field;
		}

		public void setField(String field) {
			this.field = field;
		}

	}
	// @checkstyle:on

}
