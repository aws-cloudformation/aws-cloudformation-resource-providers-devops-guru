package software.amazon.devopsguru.resourcecollection;

import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.UpdateCloudFormationCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class Translator {
    public static String AddAction = "ADD";
    public static String RemoveAction = "REMOVE";
    public static String DeleteStackNames = "*";

    /**
     * Request to create a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static UpdateResourceCollectionRequest translateToAddResourceCollectionRequest(final ResourceModel model) {
        UpdateResourceCollectionRequest updateResourceCollectionRequest;
        if (model.getResourceCollectionFilter().getCloudFormation() != null) {

            // TODO: Set this value in the Handler instead of the Request
            model.setResourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.getName());

            CloudFormationCollectionFilter cloudFormation = model.getResourceCollectionFilter().getCloudFormation();
            if (cloudFormation.getStackNames() == null || cloudFormation.getStackNames().size() == 0) {
                throw new CfnInvalidRequestException("Empty or missing stack names");
            }

            List<String> stackNames = cloudFormation.getStackNames();

            if (stackNames.contains("*") && stackNames.size() > 1) {
                throw new CfnInvalidRequestException("Star selection can only be used in isolation");
            }

            updateResourceCollectionRequest = UpdateResourceCollectionRequest.builder()
                    .resourceCollection(resourceCollectionConfigFromModel(model))
                    .build();
        } else {
            throw new CfnInvalidRequestException("Missing or invalid input");
        }
        return updateResourceCollectionRequest;
    }

    /**
     * Request to read a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static GetResourceCollectionRequest translateToGetResourceCollectionRequest(final ResourceModel model) {
        if (model.getResourceCollectionType() == null
                || !model.getResourceCollectionType().equals(ResourceCollectionType.AWS_CLOUD_FORMATION.getName())) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, "Resource not found.");
        }
        return GetResourceCollectionRequest.builder()
                .resourceCollectionType(model.getResourceCollectionType())
                .build();
    }

    /**
     * Translates resource object from sdk into a resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final GetResourceCollectionResponse awsResponse) {
        ResourceModel resourceModel = ResourceModel.builder().build();
        if (awsResponse.resourceCollection().cloudFormation() != null) {
            ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder()
                    .cloudFormation(
                            CloudFormationCollectionFilter.builder()
                                    .stackNames(awsResponse.resourceCollection().cloudFormation().stackNames())
                                    .build())
                    .build();
            resourceModel.setResourceCollectionFilter(resourceCollection);
            resourceModel.setResourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.getName());
        }
        return resourceModel;
    }

    /**
     * Translates resource object from sdk into a list of resource model
     *
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static List<ResourceModel> translateFromListResponse(final GetResourceCollectionResponse awsResponse) {
        List<ResourceModel> models = new ArrayList<ResourceModel>();
        ResourceModel resourceModel = ResourceModel.builder().build();
        if (awsResponse.resourceCollection().cloudFormation() != null) {
            ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder()
                    .cloudFormation(
                            CloudFormationCollectionFilter.builder()
                                    .stackNames(awsResponse.resourceCollection().cloudFormation().stackNames())
                                    .build())
                    .build();
            resourceModel.setResourceCollectionFilter(resourceCollection);
            resourceModel.setResourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.getName());
        }
        if(!awsResponse.resourceCollection().cloudFormation().stackNames().isEmpty()){
            models.add(resourceModel);
        }
        return models;
    }

    /**
     * Request to delete a resource
     *
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static UpdateResourceCollectionRequest translateToRemoveResourceCollectionRequest(final ResourceModel model) {
        return UpdateResourceCollectionRequest.builder()
                .resourceCollection(
                        UpdateResourceCollectionFilter.builder().cloudFormation(
                                UpdateCloudFormationCollectionFilter.builder().stackNames(Arrays.asList(DeleteStackNames)).build()
                        ).build())
                .action(RemoveAction)
                .build();
    }

    private static UpdateResourceCollectionFilter resourceCollectionConfigFromModel(final ResourceModel model) {
        List<String> stackNames = model.getResourceCollectionFilter().getCloudFormation().getStackNames();
        UpdateCloudFormationCollectionFilter updateCloudFormationCollectionFilter =
                UpdateCloudFormationCollectionFilter.builder().stackNames(stackNames).build();
        return UpdateResourceCollectionFilter.builder().cloudFormation(updateCloudFormationCollectionFilter).build();
    }
}
