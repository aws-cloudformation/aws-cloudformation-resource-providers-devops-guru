package software.amazon.devopsguru.resourcecollection;

import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.UpdateCloudFormationCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateTagCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.TagCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionType;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

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
        if (ResourceCollectionType.AWS_CLOUD_FORMATION.name().equals(model.getResourceCollectionType())) {
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
        } else if (ResourceCollectionType.AWS_TAGS.name().equals(model.getResourceCollectionType())) {
            List<TagCollection> tags = model.getResourceCollectionFilter().getTags();
            if (tags.size() != 1) {
                throw new CfnInvalidRequestException("Invalid TagCollectionFilters, only 1 TagCollection is allowed");
            }
            TagCollection tagCollection = tags.get(0);
            if (tagCollection.getAppBoundaryKey() == null
                    || tagCollection.getTagValues() == null
                    || tagCollection.getTagValues().isEmpty()
                    || !tagCollection.getAppBoundaryKey().toLowerCase().startsWith("devops-guru-")) {
                throw new CfnInvalidRequestException("Invalid AppBoundaryKey & TagValues, need to start with DevOps Guru prefix");
            }
            if (tagCollection.getTagValues().contains("*") && tagCollection.getTagValues().size() > 1) {
                throw new CfnInvalidRequestException("Star selection can only be used in isolation");
            }
            updateResourceCollectionRequest = UpdateResourceCollectionRequest.builder()
                    .resourceCollection(tagCollectionConfigFromModel(model))
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
                || (!ResourceCollectionType.AWS_CLOUD_FORMATION.name().equals(model.getResourceCollectionType())
                && !ResourceCollectionType.AWS_TAGS.name().equals(model.getResourceCollectionType()))) {
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
            resourceModel.setResourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.name());
        }
        if(!awsResponse.resourceCollection().tags().isEmpty()) {
            TagCollectionFilter tagCollectionFilter = awsResponse.resourceCollection().tags().get(0);
            TagCollection tagCollection = TagCollection.builder()
                    .appBoundaryKey(tagCollectionFilter.appBoundaryKey())
                    .tagValues(tagCollectionFilter.tagValues())
                    .build();
            ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder()
                    .tags(Arrays.asList(tagCollection))
                    .build();
            resourceModel.setResourceCollectionFilter(resourceCollection);
            resourceModel.setResourceCollectionType(ResourceCollectionType.AWS_TAGS.name());
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
        if (awsResponse == null) {
            return models;
        }

        ResourceModel resourceModel = translateFromReadResponse(awsResponse);
        if ((Objects.nonNull(awsResponse.resourceCollection().cloudFormation()) && !awsResponse.resourceCollection().cloudFormation().stackNames().isEmpty()
        ) || (!awsResponse.resourceCollection().tags().isEmpty() && !awsResponse.resourceCollection().tags().get(0).tagValues().isEmpty())) {
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
    static UpdateResourceCollectionRequest translateToRemoveCloudFormationResourceCollectionRequest(final ResourceModel model) {
        return UpdateResourceCollectionRequest.builder()
                .resourceCollection(
                        UpdateResourceCollectionFilter.builder().cloudFormation(
                                UpdateCloudFormationCollectionFilter.builder().stackNames(Arrays.asList(DeleteStackNames)).build()
                        ).build())
                .action(RemoveAction)
                .build();
    }

    static UpdateResourceCollectionRequest translateToRemoveTagsResourceCollectionRequest(final String appBoundaryKey) {
        UpdateTagCollectionFilter updateTagsFilter = UpdateTagCollectionFilter.builder()
                .appBoundaryKey(appBoundaryKey)
                .tagValues(Arrays.asList(DeleteStackNames)).build();

        return UpdateResourceCollectionRequest.builder()
                .resourceCollection(UpdateResourceCollectionFilter.builder().tags(
                        Arrays.asList(updateTagsFilter)
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


    private static UpdateResourceCollectionFilter tagCollectionConfigFromModel(final ResourceModel model) {
        TagCollection tagCollection = model.getResourceCollectionFilter().getTags().get(0);
        String appBoundaryKey = tagCollection.getAppBoundaryKey();
        List<String> tagValues = tagCollection.getTagValues();
        UpdateTagCollectionFilter updateTagsFilter = UpdateTagCollectionFilter.builder()
                .appBoundaryKey(appBoundaryKey)
                .tagValues(tagValues).build();
        return UpdateResourceCollectionFilter.builder().tags(
                Arrays.asList(updateTagsFilter)).build();
    }

}
