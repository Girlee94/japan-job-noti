module.exports = {
  apps: [
    // === Local 개발용 (bootRun) ===
    {
      name: 'ready-japan-batch',
      cwd: './',
      script: './gradlew',
      args: ':module-batch:bootRun',
      interpreter: 'none',
      env: {
        SPRING_PROFILES_ACTIVE: 'local',
      },
      autorestart: false,
      watch: false,
    },
    {
      name: 'ready-japan-api-8080',
      cwd: './',
      script: './gradlew',
      args: ':module-api:bootRun',
      interpreter: 'none',
      env: {
        SPRING_PROFILES_ACTIVE: 'local',
      },
      autorestart: false,
      watch: false,
    },
    // === Prod 환경용 (JAR 실행) ===
    // 사전에 ./gradlew :module-batch:bootJar :module-api:bootJar -x test 실행 필요
    {
      name: 'ready-japan-batch-prod',
      cwd: './',
      script: 'bash',
      args: '-c "java -jar module-batch/build/libs/module-batch-*.jar --spring.profiles.active=prod"',
      interpreter: 'none',
      env: {
        SPRING_PROFILES_ACTIVE: 'prod',
      },
      autorestart: true,
      max_restarts: 5,
      watch: false,
    },
    {
      name: 'ready-japan-api-prod',
      cwd: './',
      script: 'bash',
      args: '-c "java -jar module-api/build/libs/module-api-*.jar --spring.profiles.active=prod"',
      interpreter: 'none',
      env: {
        SPRING_PROFILES_ACTIVE: 'prod',
        PORT: '8080',
      },
      autorestart: true,
      max_restarts: 5,
      watch: false,
    },
  ],
};
