#include "FileController.h"

void FileController::postFiles(const HttpRequestPtr& req,
                               std::function<void(const HttpResponsePtr&)>&& callback)
{
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decoded = jwt::decode<traits>(token);
    if (!Headerhelper::verifyToken(decoded)) 
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }

    int user_id = decoded.get_payload_claim("Id").as_integer();
        
    if (!Headerhelper::checkRoles(decoded,"file_write"))
    {
        Headerhelper::responseCheckRoles(callback);
        return;
    }
    
    // Получаем JSON данные
    auto json = req->getJsonObject();
    if (!json) 
    {
        Headerhelper::responseCheckJson(callback);
        return;
    }

    // Извлекаем данные из JSON
    std::string body = json->get("body", "").asString();
    // Получаем массив файлов
    std::list<std::string> file_paths;
    if (!json->isMember("files_path") || !(*json)["files_path"].isArray()) 
    {
        Headerhelper::responseCheckJson(callback);
        return;
    }
    const Json::Value& filesArray = (*json)["files_path"];
    for (const auto& file : filesArray) 
    {
        std::string path = file.asString();
        if (!path.empty()) 
        {
            file_paths.push_back(path);
        }
    }

    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    FileService file(dbClient);
    auto file_data = file.createFile(body, 
                                     file_paths,
                                     user_id);

    // 3. Формируем JSON-ответ
    Json::Value jsonUser;
    jsonUser["id"] = file_data.getId();
    jsonUser["body"] = file_data.getBody();
    jsonUser["date"] = file_data.getDate();
    // Добавляем массив изображений
    Json::Value jsonImages(Json::arrayValue);
    for (const auto& image_path : file_data.getFilePaths()) 
    {
        jsonImages.append(image_path);
    }
    jsonUser["news_path"] = jsonImages;

    // 4. Создаем и настраиваем ответ
    auto resp = HttpResponse::newHttpJsonResponse(jsonUser);
    resp->setStatusCode(k201Created);
    callback(resp);
}


void FileController::getFiles(const HttpRequestPtr& req,
                              std::function<void(const HttpResponsePtr&)>&& callback)
{
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decoded = jwt::decode<traits>(token);
    if (!Headerhelper::verifyToken(decoded)) 
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }
    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    FileService file(dbClient);
    auto file_all = file.getFiles();
    // 3. Формируем JSON-ответ
    Json::Value jsonTutors;
    for (auto files : file_all)
    {
        Json::Value jsonTutor;
        jsonTutor["id"] = files.getId();
        jsonTutor["body"] = files.getBody();
        jsonTutor["date"] = files.getDate();
        // Добавляем массив изображений
        Json::Value jsonImages(Json::arrayValue);
        for (const auto& image_path : files.getFilePaths()) 
        {
            jsonImages.append(image_path);
        }

        jsonTutor["files_path"] = jsonImages; // исправлено имя поля
        jsonTutors.append(jsonTutor);
    }
    // 4. Создаем и настраиваем ответ
    auto resp = HttpResponse::newHttpJsonResponse(jsonTutors);
    resp->setStatusCode(k200OK);
    callback(resp);
}

void FileController::deleteFile(const HttpRequestPtr& req,
                                std::function<void(const HttpResponsePtr&)>&& callback, 
                                int id_file)
{
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decoded = jwt::decode<traits>(token);
    LOG_ERROR << "Зашли в deleteFile id_file "<<id_file;
    
    if (!Headerhelper::verifyToken(decoded))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }

    if (!Headerhelper::checkRoles(decoded, "file_write"))
    {
        Headerhelper::responseCheckRoles(callback);
        return;
    }

    LOG_ERROR << "Прошли проверку ";

    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    FileService file(dbClient);
    
    if (!file.deleteFile(id_file))
    {
        auto resp = HttpResponse::newHttpResponse();
        // 3. Возвращаем 204 No Content
        resp->setStatusCode(k404NotFound);
        callback(resp);
    }
    auto resp = HttpResponse::newHttpResponse();
    // 3. Возвращаем 204 No Content
    resp->setStatusCode(k204NoContent);
    callback(resp);
}