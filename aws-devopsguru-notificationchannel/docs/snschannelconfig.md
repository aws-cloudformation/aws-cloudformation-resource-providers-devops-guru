# AWS::DevOpsGuru::NotificationChannel SnsChannelConfig

Information about a notification channel configured in DevOps Guru to send notifications when insights are created.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#topicarn" title="TopicArn">TopicArn</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#topicarn" title="TopicArn">TopicArn</a>: <i>String</i>
</pre>

## Properties

#### TopicArn

_Required_: No

_Type_: String

_Minimum_: <code>36</code>

_Maximum_: <code>1024</code>

_Pattern_: <code>^arn:aws[a-z0-9-]*:sns:[a-z0-9-]+:\d{12}:[^:]+$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

