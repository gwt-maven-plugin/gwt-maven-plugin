if [ "$TRAVIS_REPO_SLUG" == "gwt-maven-plugin/gwt-maven-plugin" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then

  mvn -s ci/settings.xml deploy -DskipTests
fi
