mvn clean install
cd notes
mvn clean package
cd ..
cd reminder
mvn clean package
cd ..
cd assistant-service
mvn clean package
cd ..
docker-compose up --build