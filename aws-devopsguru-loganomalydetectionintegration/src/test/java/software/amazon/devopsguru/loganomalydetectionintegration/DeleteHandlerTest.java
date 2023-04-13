package software.amazon.devopsguru.loganomalydetectionintegration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.TagLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.TagLogGroupResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.UntagLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.UntagLogGroupResponse;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationResponse;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ResourceNotFoundException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationResponse;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<DevOpsGuruClient> proxyClient;

    @Mock
    private ProxyClient<CloudWatchLogsClient> cloudWatchLogsProxyClient;

    @Mock
    DevOpsGuruClient devOpsGuruClient;

    @Mock
    CloudWatchLogsClient cloudWatchLogsClient;

    @Mock
    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());

        devOpsGuruClient = mock(DevOpsGuruClient.class);
        proxyClient = MOCK_PROXY(proxy, devOpsGuruClient);

        cloudWatchLogsClient = mock(CloudWatchLogsClient.class);
        cloudWatchLogsProxyClient = MOCK_PROXY(proxy, cloudWatchLogsClient);

        handler.cloudWatchLogsClient = cloudWatchLogsClient;
        handler.cloudWatchLogsProxyClient = cloudWatchLogsProxyClient;
    }
    @Test
    public void handleRequest_Successful() {

        final ResourceModel model = constructResourceModel();

        final UpdateServiceIntegrationResponse updateServiceIntegrationResponse = UpdateServiceIntegrationResponse.builder().build();
        when(proxyClient.client().updateServiceIntegration(any(UpdateServiceIntegrationRequest.class))).thenReturn(updateServiceIntegrationResponse);

        final DescribeServiceIntegrationResponse describeServiceIntegrationResponse = constructDescribeServiceIntegrationResponse(OPT_IN_STATUS_ENABLED);
        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenReturn(describeServiceIntegrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(accountID)
                .build();

        TagLogGroupResponse tagLogGroupResponse = TagLogGroupResponse.builder().build();
        UntagLogGroupResponse untagLogGroupResponse = UntagLogGroupResponse.builder().build();
        when(cloudWatchLogsProxyClient.client().tagLogGroup(any(TagLogGroupRequest.class))).thenReturn(tagLogGroupResponse);
        when(cloudWatchLogsProxyClient.client().untagLogGroup(any(UntagLogGroupRequest.class))).thenReturn(untagLogGroupResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertResponse(response,model);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"DISABLED"})
    public void handleRequest_whenLogAnomalyDetectionNullOrDisabled(String optInStatus) {

        final ResourceModel model = constructResourceModel();

        final DescribeServiceIntegrationResponse describeServiceIntegrationResponse = constructDescribeServiceIntegrationResponse(optInStatus);
        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenReturn(describeServiceIntegrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(accountID)
                .build();

        TagLogGroupResponse tagLogGroupResponse = TagLogGroupResponse.builder().build();
        UntagLogGroupResponse untagLogGroupResponse = UntagLogGroupResponse.builder().build();
        when(cloudWatchLogsProxyClient.client().tagLogGroup(any(TagLogGroupRequest.class))).thenReturn(tagLogGroupResponse);
        when(cloudWatchLogsProxyClient.client().untagLogGroup(any(UntagLogGroupRequest.class))).thenReturn(untagLogGroupResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNotNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_exceptions() {

        final ResourceModel model = constructResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(accountID)
                .build();

        final DescribeServiceIntegrationResponse describeServiceIntegrationResponse = constructDescribeServiceIntegrationResponse(OPT_IN_STATUS_ENABLED);
        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenReturn(describeServiceIntegrationResponse);

        when(proxyClient.client().updateServiceIntegration(any(UpdateServiceIntegrationRequest.class))).thenThrow(AccessDeniedException.class);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateServiceIntegration(any(UpdateServiceIntegrationRequest.class))).thenThrow(InternalServerException.class);
        assertThatExceptionOfType(CfnServiceInternalErrorException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateServiceIntegration(any(UpdateServiceIntegrationRequest.class))).thenThrow(ResourceNotFoundException.class);
        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateServiceIntegration(any(UpdateServiceIntegrationRequest.class))).thenThrow(ValidationException.class);
        assertThatExceptionOfType(CfnInvalidRequestException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateServiceIntegration(any(UpdateServiceIntegrationRequest.class))).thenThrow(ThrottlingException.class);
        assertThatExceptionOfType(CfnThrottlingException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_checkForTagLogGroupPermissions_clientException() {

        final ResourceModel model = constructResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(accountID)
                .build();

        AwsServiceException clientServiceException = AwsServiceException.builder()
                .message("Mock AWS Service Client Exception")
                .build();

        when(cloudWatchLogsProxyClient.client().tagLogGroup(any(TagLogGroupRequest.class))).thenThrow(clientServiceException);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(cloudWatchLogsProxyClient.client().untagLogGroup(any(UntagLogGroupRequest.class))).thenThrow(clientServiceException);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_checkForTagLogGroupPermissions_ResourceNotFoundException() {

        final ResourceModel model = constructResourceModel();
        final UpdateServiceIntegrationResponse updateServiceIntegrationResponse = UpdateServiceIntegrationResponse.builder().build();
        when(proxyClient.client().updateServiceIntegration(any(UpdateServiceIntegrationRequest.class))).thenReturn(updateServiceIntegrationResponse);

        final DescribeServiceIntegrationResponse describeServiceIntegrationResponse = constructDescribeServiceIntegrationResponse(OPT_IN_STATUS_ENABLED);
        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenReturn(describeServiceIntegrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(accountID)
                .build();

        software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException resourceNotFoundException =
                software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException.builder().build();

        when(cloudWatchLogsProxyClient.client().tagLogGroup(any(TagLogGroupRequest.class))).thenThrow(resourceNotFoundException);
        when(cloudWatchLogsProxyClient.client().untagLogGroup(any(UntagLogGroupRequest.class))).thenThrow(resourceNotFoundException);
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        assertResponse(response,model);
    }
}
