/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.qpid.jms.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.qpid.jms.test.QpidJmsTestCase;
import org.apache.qpid.jms.test.testpeer.TestAmqpPeer;
import org.apache.qpid.jms.test.testpeer.describedtypes.sections.AmqpValueDescribedType;
import org.apache.qpid.jms.test.testpeer.matchers.sections.MessageAnnotationsSectionMatcher;
import org.apache.qpid.jms.test.testpeer.matchers.sections.MessageHeaderSectionMatcher;
import org.apache.qpid.jms.test.testpeer.matchers.sections.MessagePropertiesSectionMatcher;
import org.apache.qpid.jms.test.testpeer.matchers.sections.TransferPayloadCompositeMatcher;
import org.apache.qpid.jms.test.testpeer.matchers.types.EncodedAmqpValueMatcher;
import org.apache.qpid.proton.amqp.DescribedType;
import org.junit.Test;

public class TextMessageIntegrationTest extends QpidJmsTestCase {
    private final IntegrationTestFixture testFixture = new IntegrationTestFixture();

    @Test(timeout = 5000)
    public void testSendTextMessage() throws Exception {
        try (TestAmqpPeer testPeer = new TestAmqpPeer();) {
            Connection connection = testFixture.establishConnecton(testPeer);
            testPeer.expectBegin(true);
            testPeer.expectSenderAttach();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            String queueName = "myQueue";
            Queue queue = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(queue);

            String text = "myMessage";
            MessageHeaderSectionMatcher headersMatcher = new MessageHeaderSectionMatcher(true).withDurable(equalTo(true));
            MessageAnnotationsSectionMatcher msgAnnotationsMatcher = new MessageAnnotationsSectionMatcher(true);
            MessagePropertiesSectionMatcher propsMatcher = new MessagePropertiesSectionMatcher(true);
            TransferPayloadCompositeMatcher messageMatcher = new TransferPayloadCompositeMatcher();
            messageMatcher.setHeadersMatcher(headersMatcher);
            messageMatcher.setMessageAnnotationsMatcher(msgAnnotationsMatcher);
            messageMatcher.setPropertiesMatcher(propsMatcher);
            messageMatcher.setMessageContentMatcher(new EncodedAmqpValueMatcher(text));
            testPeer.expectTransfer(messageMatcher);

            Message message = session.createTextMessage(text);

            producer.send(message);
        }
    }

    @Test(timeout = 5000)
    public void testReceiveTextMessageWithContentAmqpValue() throws Exception {
        try (TestAmqpPeer testPeer = new TestAmqpPeer();) {
            Connection connection = testFixture.establishConnecton(testPeer);
            connection.start();

            testPeer.expectBegin(true);

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("myQueue");
            final String expectedMessageContent = "myTextMessage";

            DescribedType amqpValueStringContent = new AmqpValueDescribedType(expectedMessageContent);

            testPeer.expectReceiverAttach();
            testPeer.expectLinkFlowRespondWithTransfer(null, null, null, null, amqpValueStringContent);
            testPeer.expectDispositionThatIsAcceptedAndSettled();

            MessageConsumer messageConsumer = session.createConsumer(queue);
            Message receivedMessage = messageConsumer.receive(1000);
            testPeer.waitForAllHandlersToComplete(3000);

            assertNotNull(receivedMessage);
            assertTrue(receivedMessage instanceof TextMessage);
            assertEquals(expectedMessageContent, ((TextMessage) receivedMessage).getText());
        }
    }

    @Test(timeout = 5000)
    public void testSendTextMessageWithoutContent() throws Exception {
        try (TestAmqpPeer testPeer = new TestAmqpPeer();) {
            Connection connection = testFixture.establishConnecton(testPeer);
            testPeer.expectBegin(true);
            testPeer.expectSenderAttach();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            String queueName = "myQueue";
            Queue queue = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(queue);

            MessageHeaderSectionMatcher headersMatcher = new MessageHeaderSectionMatcher(true).withDurable(equalTo(true));
            MessageAnnotationsSectionMatcher msgAnnotationsMatcher = new MessageAnnotationsSectionMatcher(true);
            MessagePropertiesSectionMatcher propsMatcher = new MessagePropertiesSectionMatcher(true);
            TransferPayloadCompositeMatcher messageMatcher = new TransferPayloadCompositeMatcher();
            messageMatcher.setHeadersMatcher(headersMatcher);
            messageMatcher.setMessageAnnotationsMatcher(msgAnnotationsMatcher);
            messageMatcher.setPropertiesMatcher(propsMatcher);
            messageMatcher.setMessageContentMatcher(new EncodedAmqpValueMatcher(null));
            testPeer.expectTransfer(messageMatcher);

            Message message = session.createTextMessage();

            producer.send(message);
        }
    }

    @Test(timeout = 5000)
    public void testReceiveTextMessageWithAmqpValueNullBodyAndNoMsgTypeAnnotation() throws Exception {
        try (TestAmqpPeer testPeer = new TestAmqpPeer();) {
            Connection connection = testFixture.establishConnecton(testPeer);
            connection.start();

            testPeer.expectBegin(true);

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("myQueue");

            DescribedType amqpValueNullContent = new AmqpValueDescribedType(null);

            testPeer.expectReceiverAttach();
            testPeer.expectLinkFlowRespondWithTransfer(null, null, null, null, amqpValueNullContent);
            testPeer.expectDispositionThatIsAcceptedAndSettled();

            MessageConsumer messageConsumer = session.createConsumer(queue);
            Message receivedMessage = messageConsumer.receive(1000);
            testPeer.waitForAllHandlersToComplete(3000);

            assertNotNull(receivedMessage);
            assertTrue(receivedMessage instanceof TextMessage);
            assertNull(((TextMessage) receivedMessage).getText());
        }
    }
}
