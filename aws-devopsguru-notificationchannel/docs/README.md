# AWS::DevOpsGuru::NotificationChannel

This resource schema represents the NotificationChannel resource in the Amazon DevOps Guru.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::DevOpsGuru::NotificationChannel",
    "Properties" : {
        "<a href="#config" title="Config">Config</a>" : <i><a href="notificationchannelconfig.md">NotificationChannelConfig</a></i>,
    }
}
</pre>

### YAML

<pre>
Type: AWS::DevOpsGuru::NotificationChannel
Properties:
    <a href="#config" title="Config">Config</a>: <i><a href="notificationchannelconfig.md">NotificationChannelConfig</a></i>
</pre>

## Properties

#### Config

Information about notification channels you have configured with DevOps Guru.

_Required_: Yes

_Type_: <a href="notificationchannelconfig.md">NotificationChannelConfig</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Id.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Id

The ID of a notification channel.

