setup:
	cd AppApplication && ./gradlew build -x test

start:
	cd AppApplication && ./gradlew bootRun
