package software.amazon.devopsguru.loganomalydetectionintegration;

import java.time.Duration;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationResponse;
import software.amazon.awssdk.services.devopsguru.model.LogsAnomalyDetectionIntegration;
import software.amazon.awssdk.services.devopsguru.model.ServiceIntegrationConfig;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
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
    DevOpsGuruClient devOpsGuruClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        devOpsGuruClient = mock(DevOpsGuruClient.class);
        proxyClient = MOCK_PROXY(proxy, devOpsGuruClient);
    }

    @Test
    public void handleRequest_Successful() {
        final ReadHandler handler = new ReadHandler();
        final ResourceModel model = constructResourceModel();
        final DescribeServiceIntegrationResponse describeServiceIntegrationResponse = constructDescribeServiceIntegrationResponse(OPT_IN_STATUS_ENABLED);
        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenReturn(describeServiceIntegrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(accountID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertResponse(response,model);
    }

    @Test
    public void handleRequest_whenLogAnomalyDetectionDisabled() {
        final ReadHandler handler = new ReadHandler();
        final ResourceModel model = constructResourceModel();
        final DescribeServiceIntegrationResponse describeServiceIntegrationResponse =
                constructDescribeServiceIntegrationResponse(OPT_IN_STATUS_DISABLED);
        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class)))
                .thenReturn(describeServiceIntegrationResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(accountID)
                .build();

        assertThatExceptionOfType(CfnNotFoundException.class)
                .isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_exceptions() {
        final ReadHandler handler = new ReadHandler();

        final ResourceModel model = constructResourceModel();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .awsAccountId(accountID)
                .build();

        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenThrow(AccessDeniedException.class);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenThrow(InternalServerException.class);
        assertThatExceptionOfType(CfnServiceInternalErrorException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenThrow(ResourceNotFoundException.class);
        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenThrow(ValidationException.class);
        assertThatExceptionOfType(CfnInvalidRequestException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().describeServiceIntegration(any(DescribeServiceIntegrationRequest.class))).thenThrow(ThrottlingException.class);
        assertThatExceptionOfType(CfnThrottlingException.class).isThrownBy(()->handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }
}
