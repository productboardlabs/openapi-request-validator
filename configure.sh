#!/bin/bash

MAVEN_SETTINGS=$MAVEN_HOME/conf/settings.xml

echo "Messing up with Maven settings:"
echo $MAVEN_SETTINGS

sed -i'back' '/<servers>/ a\
<server><id>atlassian-private</id><username>bitbucketci</username><password>'"$MAVEN_REPO_PASSWORD"'</password></server>' $MAVEN_SETTINGS

sed -i'bak' '/<profiles>/ a\
<profile><id>atlassian-private</id><activation><activeByDefault>true</activeByDefault></activation><repositories><repository><id>atlassian-private</id><name>Atlassian Private</name><url>https://maven.atlassian.com/content/repositories/atlassian-private/</url><layout>default</layout></repository></repositories><pluginRepositories><pluginRepository><id>atlassian-private</id><url>https://maven.atlassian.com/content/groups/internal</url></pluginRepository></pluginRepositories></profile>' $MAVEN_SETTINGS

sed -i'bak' '/<profiles>/ a\
<profile><id>atlassian-public</id><activation><activeByDefault>true</activeByDefault></activation><repositories><repository><id>atlassian-public</id><url>https://maven.atlassian.com/repository/public</url></repository></repositories></profile>' $MAVEN_SETTINGS