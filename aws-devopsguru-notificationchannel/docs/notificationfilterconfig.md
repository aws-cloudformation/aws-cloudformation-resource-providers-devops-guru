# AWS::DevOpsGuru::NotificationChannel NotificationFilterConfig

Information about filters of a notification channel configured in DevOpsGuru to filter for insights.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#severities" title="Severities">Severities</a>" : <i>[ String, ... ]</i>,
    "<a href="#messagetypes" title="MessageTypes">MessageTypes</a>" : <i>[ String, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#severities" title="Severities">Severities</a>: <i>
      - String</i>
<a href="#messagetypes" title="MessageTypes">MessageTypes</a>: <i>
      - String</i>
</pre>

## Properties

#### Severities

DevOps Guru insight severities to filter for

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### MessageTypes

DevOps Guru message types to filter for

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
