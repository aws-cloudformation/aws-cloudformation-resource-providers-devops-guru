package software.amazon.devopsguru.resourcecollection;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ResourceNotFoundException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.UpdateCloudFormationCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    protected static final Constant BACKOFF_STRATEGY =
            Constant.of().timeout(Duration.ofMinutes(5L)).delay(Duration.ofSeconds(10L)).build();
    protected static final int MAX_STACK_NAME_NUMBER_PER_API_CALL = 100;

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(DevOpsGuruClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final Logger logger);

    protected GetResourceCollectionResponse getSingleResourceCollection(
            final GetResourceCollectionRequest getResourceCollectionRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model) {
        GetResourceCollectionRequest awsRequest = getResourceCollectionRequest;

        try {
            GetResourceCollectionResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest,
                    proxyClient.client()::getResourceCollection);
            if (awsResponse.resourceCollection().cloudFormation() == null) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getResourceCollectionType());
            }
            return awsResponse;
        } catch (final AccessDeniedException e) {
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getResourceCollectionType(), e);
        }
    }

    protected List<String> getAllResourceCollection(
            final GetResourceCollectionRequest getResourceCollectionRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model,
            final Logger logger) {
        GetResourceCollectionResponse awsResponse = null;
        GetResourceCollectionRequest awsRequest = getResourceCollectionRequest;
        List<String> stackNamesList = new ArrayList<>();

        do {
            awsResponse = getSingleResourceCollection(awsRequest, proxyClient, model);
            stackNamesList.addAll(awsResponse.resourceCollection().cloudFormation().stackNames());
            awsRequest = GetResourceCollectionRequest.builder()
                    .resourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.getName())
                    .nextToken(awsResponse.nextToken())
                    .build();
        } while (awsResponse.nextToken() != null);

        // Interpret empty stacks as RNF
        if (stackNamesList.isEmpty()) {
            logger.log("Empty resource collection. Throwing NotFoundException");
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getResourceCollectionType());
        }

        return stackNamesList;
    }

    protected void checkIsEmptyResourceCollection(
            final GetResourceCollectionRequest getResourceCollectionRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model,
            final Logger logger) {
        GetResourceCollectionResponse getResourceCollectionResponse = getSingleResourceCollection(getResourceCollectionRequest, proxyClient, model);
        if (getResourceCollectionResponse.resourceCollection().cloudFormation().stackNames().isEmpty()) {
            logger.log("Empty resource collection. Throwing NotFoundException");
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getResourceCollectionType());
        };
    }

    protected UpdateResourceCollectionResponse updateResourceCollection(
            final UpdateResourceCollectionRequest updateResourceCollectionRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model,
            final Logger logger,
            final boolean overrideEmpty) {
        UpdateResourceCollectionResponse updateResourceCollectionResponse = null;
        List<String> getResourceCollectionResponseStackNames = null;

        try {
            GetResourceCollectionRequest getResourceCollectionRequest =
                    GetResourceCollectionRequest.builder().resourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.getName()).build();
            getResourceCollectionResponseStackNames = getAllResourceCollection(getResourceCollectionRequest,
                    proxyClient, model, logger);
            logger.log(String.format("getAllResourceCollection return %s", getResourceCollectionResponseStackNames));
        } catch (CfnNotFoundException e) {
            if (overrideEmpty) {
                getResourceCollectionResponseStackNames = new ArrayList<>();
                logger.log("This is user first time onboarding. Setting existing list as empty.");
            } else {
                logger.log("Attempting to update non-existent resource");
                throw e;
            }
        }
        List<String> updateStackNames =
                updateResourceCollectionRequest.resourceCollection().cloudFormation().stackNames();

        Set<String> updateStackNamesSet = new HashSet<>(updateStackNames);
        Set<String> existingStackNamesSet = new HashSet<>(getResourceCollectionResponseStackNames);
        List<String> addStackNames = new ArrayList<>(Sets.difference(updateStackNamesSet, existingStackNamesSet));
        List<String> removeStackNames = new ArrayList<>(Sets.difference(existingStackNamesSet, updateStackNamesSet));

        if (addStackNames.size() > 0) {
            updateResourceCollectionResponse = updateResourceCollectionByBatch(addStackNames, Translator.AddAction,
                    proxyClient, logger);
        }

        if (removeStackNames.size() > 0 && !existingStackNamesSet.contains("*") && !updateStackNamesSet.contains("*")) {
            updateResourceCollectionResponse = updateResourceCollectionByBatch(removeStackNames,
                    Translator.RemoveAction, proxyClient, logger);
        }
        if (updateResourceCollectionResponse == null)
            return UpdateResourceCollectionResponse.builder().build();
        else
            return updateResourceCollectionResponse;
    }

    protected UpdateResourceCollectionResponse updateResourceCollectionByBatch(
            final List<String> updateResourceCollectionStackNamesList,
            final String action,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final Logger logger) {
        UpdateResourceCollectionResponse awsResponse = null;
        logger.log(String.format("UpdateResourceCollectionByBatch of action [%s] and stacks [%s]", action, updateResourceCollectionStackNamesList));
        try {
            List<String> stackNames = updateResourceCollectionStackNamesList;
            for (List<String> stackNamesPartition : Lists.partition(stackNames, MAX_STACK_NAME_NUMBER_PER_API_CALL)) {
                UpdateResourceCollectionFilter updateResourceCollectionFilter =
                        UpdateResourceCollectionFilter.builder().cloudFormation(UpdateCloudFormationCollectionFilter.builder().stackNames(stackNamesPartition).build()).build();
                UpdateResourceCollectionRequest tempRequest = UpdateResourceCollectionRequest.builder()
                        .action(action)
                        .resourceCollection(updateResourceCollectionFilter)
                        .build();
                awsResponse = proxyClient.injectCredentialsAndInvokeV2(tempRequest,
                        proxyClient.client()::updateResourceCollection);
                logger.log(String.format("UpdateResourceCollectionByBatch response: %s", awsResponse.toString()));
            }
        } catch (final AccessDeniedException e) {
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        }

        return awsResponse;
    }
}
