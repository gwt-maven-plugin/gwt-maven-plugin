if [ "$TRAVIS_REPO_SLUG" == "gwt-maven-plugin/gwt-maven-plugin" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk7" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then

  echo -e "$GITHUB_KEY" > ~/.ssh/github_key
  # echo -e "$GITHUB_PUB" > ~/.ssh/github.pub
  chmod 600 ~/.ssh/github*

  eval `ssh-agent -s`
  ssh-add ~/.ssh/github_key

  git config --global user.email "gwt-maven-plugin@travis-ci.org"
  git config --global user.name "GWT Maven Plugin at Travis CI"

  mvn -s ci/settings.xml -DrelativizeDecorationLinks=false site site:stage-deploy
fi
