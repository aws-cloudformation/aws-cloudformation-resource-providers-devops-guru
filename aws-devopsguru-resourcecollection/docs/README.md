# AWS::DevOpsGuru::ResourceCollection

This resource schema represents the ResourceCollection resource in the Amazon DevOps Guru.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::DevOpsGuru::ResourceCollection",
    "Properties" : {
        "<a href="#resourcecollectionfilter" title="ResourceCollectionFilter">ResourceCollectionFilter</a>" : <i><a href="resourcecollectionfilter.md">ResourceCollectionFilter</a></i>,
    }
}
</pre>

### YAML

<pre>
Type: AWS::DevOpsGuru::ResourceCollection
Properties:
    <a href="#resourcecollectionfilter" title="ResourceCollectionFilter">ResourceCollectionFilter</a>: <i><a href="resourcecollectionfilter.md">ResourceCollectionFilter</a></i>
</pre>

## Properties

#### ResourceCollectionFilter

Information about a filter used to specify which AWS resources are analyzed for anomalous behavior by DevOps Guru.

_Required_: Yes

_Type_: <a href="resourcecollectionfilter.md">ResourceCollectionFilter</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the ResourceCollectionType.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### ResourceCollectionType

The type of ResourceCollection
