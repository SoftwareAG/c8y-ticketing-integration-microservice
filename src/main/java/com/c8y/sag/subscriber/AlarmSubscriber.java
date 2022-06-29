package com.c8y.sag.subscriber;

import com.cumulocity.sdk.client.PlatformParameters;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.notification.SubscribeOperationListener;
import com.cumulocity.sdk.client.notification.Subscriber;
import com.cumulocity.sdk.client.notification.SubscriberBuilder;
import com.cumulocity.sdk.client.notification.Subscription;
import com.cumulocity.sdk.client.notification.SubscriptionListener;
import com.cumulocity.sdk.client.notification.SubscriptionNameResolver;

/**
 * 
 * @author Kalpshekhar Gupta
 *
 */

public class AlarmSubscriber implements Subscriber<String, AlarmNotification> {
	
	private static final String REALTIME_NOTIFICATIONS_URL = "cep/realtime";
	
	private static final String CHANNEL_PREFIX = "/alarms/";
	
	private final Subscriber<String, AlarmNotification> subscriber;
	
	public AlarmSubscriber(PlatformParameters parameters) {
        subscriber = createSubscriber(parameters);
    }
	
	private Subscriber<String, AlarmNotification> createSubscriber(PlatformParameters parameters) {
        return SubscriberBuilder.<String, AlarmNotification>anSubscriber()
                .withParameters(parameters)
                .withEndpoint(REALTIME_NOTIFICATIONS_URL)
                .withSubscriptionNameResolver(new Identity())
                .withDataType(AlarmNotification.class)
                .build();
    }

	@Override
	public void disconnect() {
		subscriber.disconnect();
	}

	@Override
	public Subscription<String> subscribe(String channelId, SubscriptionListener<String, AlarmNotification> listener)
			throws SDKException {
		return subscriber.subscribe(CHANNEL_PREFIX + channelId, listener);
	}

	@Override
	public Subscription<String> subscribe(String arg0, SubscribeOperationListener arg1,
			SubscriptionListener<String, AlarmNotification> arg2, boolean arg3) throws SDKException {
		return null;
	}
	
	private static final class Identity implements SubscriptionNameResolver<String> {
        @Override
        public String apply(String id) {
            return id;
        }
    }
	
}
