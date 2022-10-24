package software.amazon.devopsguru.notificationchannel;


import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsRequest;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsResponse;
import software.amazon.awssdk.services.devopsguru.model.NotificationChannel;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {
    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<DevOpsGuruClient> proxyClient;

    @Mock
    DevOpsGuruClient sdkClient;

    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(DevOpsGuruClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @ParameterizedTest(name = "run #{index} with [{arguments}]")
    @MethodSource("validationNotificationChannelWithResourceModelParams")
    public void handleRequest_successful(final Pair<List<NotificationChannel>, ResourceModel> mappingPair) {
        final NotificationChannel channel = mappingPair.getKey().get(0);
        final ResourceModel model = mappingPair.getValue();
        final List<NotificationChannel> channels = Arrays.asList(channel);

        final ListNotificationChannelsResponse listNotificationChannelsResponse = ListNotificationChannelsResponse.builder().channels(channels).build();
        when(proxyClient.client().listNotificationChannels(any(ListNotificationChannelsRequest.class))).thenReturn(listNotificationChannelsResponse);


        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertResponse(response, model);
    }

    @Test
    public void handleRequest_IdNotInTheListResponse() {
        final NotificationChannel channel = constructNotificationChannel(topicArn, id, insightSeveritiesFilter, messageTypesFilter);
        final List<NotificationChannel> channels = Arrays.asList(channel);
        final ListNotificationChannelsResponse listNotificationChannelsResponse =
                ListNotificationChannelsResponse.builder().channels(channels).build();
        when(proxyClient.client().listNotificationChannels(any(ListNotificationChannelsRequest.class))).thenReturn(listNotificationChannelsResponse);

        final ResourceModel model = constructResourceModel(topicArn1, id1, insightSeveritiesFilter, messageTypesFilter);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(() -> handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger));
    }

}
