package software.amazon.devopsguru.resourcecollection;

import java.time.Duration;
import java.util.Arrays;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.TagCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionType;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.CloudFormationCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<DevOpsGuruClient> proxyClient;

    @Mock
    DevOpsGuruClient sdkClient;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(DevOpsGuruClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @Test
    public void handleRequest_CloudFormation_SimpleSuccess() {
        final CloudFormationCollectionFilter cloudFormationCollection = CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName")).build();
        final ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse = GetResourceCollectionResponse.builder().resourceCollection(resourceCollection).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.name())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollectionFilter = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel responseModel = ResourceModel.builder()
                .resourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.name())
                .resourceCollectionFilter(model_resourceCollectionFilter)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels().get(0)).isEqualTo(responseModel);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CloudFormation_exceptions() {
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.name()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenThrow(AccessDeniedException.class);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenThrow(ThrottlingException.class);
        assertThatExceptionOfType(CfnThrottlingException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenThrow(InternalServerException.class);
        assertThatExceptionOfType(CfnServiceInternalErrorException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }

    @Test
    public void handleRequest_Tags_SimpleSuccess() {
        final TagCollectionFilter tagCollectionFilter =
                TagCollectionFilter.builder()
                        .appBoundaryKey("DevOps-Guru-TagKey")
                        .tagValues(Arrays.asList("TagName")).build();
        final software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter resourceCollectionSdk =
                software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter.builder().tags(tagCollectionFilter).build();
        final GetResourceCollectionResponse getResourceCollectionResponse =
                GetResourceCollectionResponse.builder().resourceCollection(resourceCollectionSdk).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionType(ResourceCollectionType.AWS_TAGS.name())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        final software.amazon.devopsguru.resourcecollection.TagCollection tagCollection =
                software.amazon.devopsguru.resourcecollection.TagCollection.builder()
                        .appBoundaryKey("DevOps-Guru-TagKey")
                        .tagValues(Arrays.asList("TagName")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollectionFilter =
                software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().tags(Arrays.asList(tagCollection)).build();

        final ResourceModel responseModel = ResourceModel.builder()
                .resourceCollectionType(ResourceCollectionType.AWS_TAGS.name())
                .resourceCollectionFilter(model_resourceCollectionFilter)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels().get(0)).isEqualTo(responseModel);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Tags_exceptions() {
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionType(ResourceCollectionType.AWS_TAGS.name()).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenThrow(AccessDeniedException.class);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenThrow(ThrottlingException.class);
        assertThatExceptionOfType(CfnThrottlingException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenThrow(InternalServerException.class);
        assertThatExceptionOfType(CfnServiceInternalErrorException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

    }


}
