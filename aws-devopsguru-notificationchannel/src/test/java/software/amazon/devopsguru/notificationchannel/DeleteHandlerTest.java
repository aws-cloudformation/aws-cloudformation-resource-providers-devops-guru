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
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsRequest;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsResponse;
import software.amazon.awssdk.services.devopsguru.model.NotificationChannel;
import software.amazon.awssdk.services.devopsguru.model.RemoveNotificationChannelRequest;
import software.amazon.awssdk.services.devopsguru.model.RemoveNotificationChannelResponse;
import software.amazon.awssdk.services.devopsguru.model.ResourceNotFoundException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<DevOpsGuruClient> proxyClient;

    @Mock
    DevOpsGuruClient sdkClient;

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(DevOpsGuruClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @ParameterizedTest(name = "run #{index} with [{arguments}]")
    @MethodSource("validationNotificationChannelWithResourceModelParams")
    public void handleRequest(final Pair<List<NotificationChannel>, ResourceModel> mappingPair) {
        final NotificationChannel channel = mappingPair.getKey().get(0);
        final ResourceModel model = mappingPair.getValue();
        final List<NotificationChannel> channels = Arrays.asList(channel);

        final ListNotificationChannelsResponse listNotificationChannelsResponse = ListNotificationChannelsResponse.builder().channels(channels).build();
        when(proxyClient.client().listNotificationChannels(any(ListNotificationChannelsRequest.class))).thenReturn(listNotificationChannelsResponse);
        final RemoveNotificationChannelResponse deleteNotificationChannelResponse = RemoveNotificationChannelResponse.builder().build();
        when(proxyClient.client().removeNotificationChannel(any(RemoveNotificationChannelRequest.class))).thenReturn(deleteNotificationChannelResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).removeNotificationChannel(any(RemoveNotificationChannelRequest.class));
    }

    @Test
    public void handleRequest_FailWhenNotFoundException_checkForPreDeleteResourceExistence() {
        NotificationChannel channel = constructNotificationChannel(topicArn, id, insightSeveritiesFilter, messageTypesFilter);
        List<NotificationChannel> channels = Arrays.asList(channel);
        final ListNotificationChannelsResponse listNotificationChannelsResponse = ListNotificationChannelsResponse.builder().channels(channels).build();
        when(proxyClient.client().listNotificationChannels(any(ListNotificationChannelsRequest.class))).thenReturn(listNotificationChannelsResponse);

        final ResourceModel model = constructResourceModel(topicArn1, id1, insightSeveritiesFilter, messageTypesFilter);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Exceptions() {
        NotificationChannel channel = constructNotificationChannel(topicArn, id, insightSeveritiesFilter, messageTypesFilter);
        List<NotificationChannel> channels = Arrays.asList(channel);
        final ListNotificationChannelsResponse listNotificationChannelsResponse = ListNotificationChannelsResponse.builder().channels(channels).build();
        when(proxyClient.client().listNotificationChannels(any(ListNotificationChannelsRequest.class))).thenReturn(listNotificationChannelsResponse);

        final ResourceModel model = constructResourceModel(topicArn, id, insightSeveritiesFilter, messageTypesFilter);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().removeNotificationChannel(any(RemoveNotificationChannelRequest.class))).thenThrow(AccessDeniedException.class);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().removeNotificationChannel(any(RemoveNotificationChannelRequest.class))).thenThrow(InternalServerException.class);
        assertThatExceptionOfType(CfnServiceInternalErrorException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().removeNotificationChannel(any(RemoveNotificationChannelRequest.class))).thenThrow(ResourceNotFoundException.class);
        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().removeNotificationChannel(any(RemoveNotificationChannelRequest.class))).thenThrow(ValidationException.class);
        assertThatExceptionOfType(CfnInvalidRequestException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().removeNotificationChannel(any(RemoveNotificationChannelRequest.class))).thenThrow(ThrottlingException.class);
        assertThatExceptionOfType(CfnThrottlingException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }
}
