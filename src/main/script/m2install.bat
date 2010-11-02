set GWT_VERSION=1.6.0
set GWT_WINDOWS_HOME=C:\private\usr\Google\gwt-windows-%GWT_VERSION%

call mvn install:install-file "-Dfile=%GWT_WINDOWS_HOME%\gwt-user.jar" -DgroupId=com.google.gwt -DartifactId=gwt-user -Dversion=%GWT_VERSION% -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true
call mvn install:install-file "-Dfile=%GWT_WINDOWS_HOME%\gwt-servlet.jar" -DgroupId=com.google.gwt -DartifactId=gwt-servlet -Dversion=%GWT_VERSION% -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true
call mvn install:install-file "-Dfile=%GWT_WINDOWS_HOME%\gwt-dev-windows.jar" -DgroupId=com.google.gwt -DartifactId=gwt-dev-windows -Dversion=%GWT_VERSION% -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true
