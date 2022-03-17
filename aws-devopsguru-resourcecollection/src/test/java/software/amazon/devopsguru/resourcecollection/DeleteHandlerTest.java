package software.amazon.devopsguru.resourcecollection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.TagCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionType;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.CloudFormationCollectionFilter;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    @Test
    public void handleRequest_CloudFormation_SimpleSuccess() {
        final CloudFormationCollectionFilter cloudFormationCollection =
                CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName")).build();
        final software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter resourceCollectionSdk =
                software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse =
                GetResourceCollectionResponse.builder().resourceCollection(resourceCollectionSdk).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final UpdateResourceCollectionResponse updateResourceCollectionResponse =
                UpdateResourceCollectionResponse.builder().build();
        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenReturn(updateResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Collections.emptyList()).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter resourceCollectionFilter =
                software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(resourceCollectionFilter)
                .resourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.name())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).updateResourceCollection(any(UpdateResourceCollectionRequest.class));
    }

    @Test
    public void handleRequest_CloudFormation_Exceptions() {
        final CloudFormationCollectionFilter cloudFormationCollection =
                CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName")).build();
        final software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter resourceCollectionSdk =
                software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse =
                GetResourceCollectionResponse.builder().resourceCollection(resourceCollectionSdk).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter resourceCollection =
                software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(resourceCollection)
                .resourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.name())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(AccessDeniedException.class);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(() -> handler.handleRequest(proxy,
                request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(ThrottlingException.class);
        assertThatExceptionOfType(CfnThrottlingException.class).isThrownBy(() -> handler.handleRequest(proxy, request
                , new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(InternalServerException.class);
        assertThatExceptionOfType(CfnServiceInternalErrorException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(ValidationException.class);
        assertThatExceptionOfType(CfnInvalidRequestException.class).isThrownBy(() -> handler.handleRequest(proxy,
                request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Tags_SimpleSuccess() {
        final TagCollectionFilter tagCollectionFilter =
                TagCollectionFilter.builder()
                        .appBoundaryKey("TagKey")
                        .tagValues(Arrays.asList("TagName")).build();
        final software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter resourceCollectionSdk =
                software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter.builder().tags(tagCollectionFilter).build();
        final GetResourceCollectionResponse getResourceCollectionResponse =
                GetResourceCollectionResponse.builder().resourceCollection(resourceCollectionSdk).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final UpdateResourceCollectionResponse updateResourceCollectionResponse =
                UpdateResourceCollectionResponse.builder().build();
        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenReturn(updateResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.TagCollection tagCollection =
                software.amazon.devopsguru.resourcecollection.TagCollection.builder()
                        .appBoundaryKey("TagKey")
                        .tagValues(Arrays.asList("TagName")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter resourceCollection =
                software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().tags(Arrays.asList(tagCollection)).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(resourceCollection)
                .resourceCollectionType(ResourceCollectionType.AWS_TAGS.name())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).updateResourceCollection(any(UpdateResourceCollectionRequest.class));
    }

    @Test
    public void handleRequest_Tags_Exceptions() {
        final TagCollectionFilter tagCollectionFilter =
                TagCollectionFilter.builder()
                        .appBoundaryKey("TagKey")
                        .tagValues(Arrays.asList("TagName")).build();
        final software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter resourceCollectionSdk =
                software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter.builder().tags(tagCollectionFilter).build();
        final GetResourceCollectionResponse getResourceCollectionResponse =
                GetResourceCollectionResponse.builder().resourceCollection(resourceCollectionSdk).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.TagCollection tagCollection =
                software.amazon.devopsguru.resourcecollection.TagCollection.builder()
                        .appBoundaryKey("TagKey")
                        .tagValues(Arrays.asList("TagName")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter resourceCollection =
                software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().tags(Arrays.asList(tagCollection)).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(resourceCollection)
                .resourceCollectionType(ResourceCollectionType.AWS_TAGS.name())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(AccessDeniedException.class);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(() -> handler.handleRequest(proxy,
                request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(ThrottlingException.class);
        assertThatExceptionOfType(CfnThrottlingException.class).isThrownBy(() -> handler.handleRequest(proxy, request
                , new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(InternalServerException.class);
        assertThatExceptionOfType(CfnServiceInternalErrorException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(ValidationException.class);
        assertThatExceptionOfType(CfnInvalidRequestException.class).isThrownBy(() -> handler.handleRequest(proxy,
                request, new CallbackContext(), proxyClient, logger));
    }
}

