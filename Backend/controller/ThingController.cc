#include "ThingController.h"

void ThingController::postThing(const HttpRequestPtr& req,
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

    // Получаем JSON данные
    auto json = req->getJsonObject();
    if (!json) 
    {
        Headerhelper::responseCheckJson(callback);
        return;
    }
    // Извлекаем данные из JSON
    std::string type = json->get("type", "").asString();
    std::string body = json->get("body", "").asString();
    int room = json->get("room", "").asInt();
    // Получаем массив файлов
    std::list<std::string> thing_paths;
    if (!json->isMember("thing_paths") || !(*json)["thing_paths"].isArray()) 
    {
        Headerhelper::responseCheckJson(callback);
        return;
    }
    const Json::Value& filesArray = (*json)["thing_paths"];
    for (const auto& file : filesArray) 
    {
        std::string path = file.asString();
        if (!path.empty()) 
        {
            thing_paths.push_back(path);
        }
    }

    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    ThingService thing(dbClient);
    
    auto thing_data = thing.createThing(type, 
                                        body, 
                                        room,
                                        thing_paths,
                                        user_id);

    // 3. Формируем JSON-ответ
    Json::Value jsonThing;
    jsonThing["id"] = thing_data.getId();
    jsonThing["type"] = thing_data.getType();
    jsonThing["body"] = thing_data.getBody();
    jsonThing["room"] = thing_data.getRoom();
    jsonThing["date"] = thing_data.getDate();
    // Добавляем массив изображений
        Json::Value jsonImages(Json::arrayValue);
        for (const auto& image_path : thing_data.getFilePaths()) 
        {
            jsonImages.append(image_path);
        }
        jsonThing["thing_path"] = jsonImages;

    // 4. Создаем и настраиваем ответ
    auto resp = HttpResponse::newHttpJsonResponse(jsonThing);
    resp->setStatusCode(k201Created);
    callback(resp);
}


void ThingController::getThings(const HttpRequestPtr& req,
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
    ThingService thing(dbClient);
    auto thing_all = thing.getThings();
    // 3. Формируем JSON-ответ
    Json::Value jsonThings;
    for (auto files : thing_all)
    {
        Json::Value jsonThing;
        jsonThing["id"] = files.getId();
        jsonThing["type"] = files.getType();
        jsonThing["body"] = files.getBody();
        jsonThing["room"] = files.getRoom();
        jsonThing["date"] = files.getDate();
        // Добавляем массив изображений
        Json::Value jsonImages(Json::arrayValue);
        for (const auto& image_path : files.getFilePaths()) 
        {
            jsonImages.append(image_path);
        }

        jsonThing["files_path"] = jsonImages; // исправлено имя поля
        jsonThings.append(jsonThing);
    }
    // 4. Создаем и настраиваем ответ
    auto resp = HttpResponse::newHttpJsonResponse(jsonThings);
    resp->setStatusCode(k200OK);
    callback(resp);
}

void ThingController::deleteThing(const HttpRequestPtr& req,
                                  std::function<void(const HttpResponsePtr&)>&& callback, 
                                  int id_thing,
                                  int id_user)
{
    LOG_ERROR << "Зашли в метод deleteThing id_thing "<<id_thing;
    LOG_ERROR << "Зашли в метод deleteThing id_user "<<id_user;
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decoded = jwt::decode<traits>(token);
    
    if (!Headerhelper::verifyToken(decoded))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }
    
    if (!Headerhelper::checkRoles(decoded, "thing_write") && id_user != decoded.get_payload_claim("Id").as_integer())
    {
        Headerhelper::responseCheckRoles(callback);
        return;
    }

    LOG_ERROR << "Прошли проверку роли";
    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    ThingService thing(dbClient);
    
    bool result = thing.deleteThing(id_thing);
    
    if (!result)
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