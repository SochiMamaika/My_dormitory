#include "RepairController.h"

void RepairController::postRepair(const HttpRequestPtr& req,
                                  std::function<void(const HttpResponsePtr&)>&& callback)
{
    LOG_ERROR << "Зашли в метод postRepair";
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
    int user_id_from_front = json->get("user_id", "").asInt();
    std::string type = json->get("type", "").asString();
    std::string body = json->get("body", "").asString();
    int room = json->get("room", "").asInt();
    // Получаем массив файлов
    std::list<std::string> repair_paths;
    if (!json->isMember("repair_paths") || !(*json)["repair_paths"].isArray()) 
    {
        Headerhelper::responseCheckJson(callback);
        return;
    }
    const Json::Value& filesArray = (*json)["repair_paths"];
    for (const auto& file : filesArray) 
    {
        std::string path = file.asString();
        if (!path.empty()) 
        {
            repair_paths.push_back(path);
        }
    }

    if (type != "plumber" && type != "carpenter" && type != "electrician")
    {
        auto resp = HttpResponse::newHttpResponse();
        resp->setStatusCode(k400BadRequest);
        callback(resp);
    }

    if (user_id_from_front != user_id)
    {
        auto resp = HttpResponse::newHttpResponse();
        resp->setStatusCode(k400BadRequest);
        callback(resp);
    }

    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    RepairService repair(dbClient);
    auto repair_data = repair.createRepair(type, 
                                           body, 
                                           room,
                                           repair_paths,
                                           user_id);

    // 3. Формируем JSON-ответ
    Json::Value jsonRepair;
    jsonRepair["id"] = repair_data.getId();
    jsonRepair["type"] = repair_data.getType();
    jsonRepair["body"] = repair_data.getBody();
    jsonRepair["date"] = repair_data.getDate();
    jsonRepair["room"] = repair_data.getRoom();
    jsonRepair["activity"] = repair_data.getActivity();
    // Добавляем массив изображений
    Json::Value jsonImages(Json::arrayValue);
    for (const auto& repair_path : repair_data.getRepairPaths()) 
    {
        jsonImages.append(repair_path);
    }
    jsonRepair["repair_path"] = jsonImages;

    // 4. Создаем и настраиваем ответ
    auto resp = HttpResponse::newHttpJsonResponse(jsonRepair);
    resp->setStatusCode(k201Created);
    callback(resp);
}


void RepairController::getMyRepairs(const HttpRequestPtr& req,
                            std::function<void(const HttpResponsePtr&)>&& callback)
{
    LOG_ERROR << "Зашли в getMyRepair для ремонтника";
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decoded = jwt::decode<traits>(token);
    if (!Headerhelper::verifyToken(decoded)) 
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }
    int user_id = decoded.get_payload_claim("Id").as_integer();
    LOG_ERROR << "получили user_id "<<user_id;
    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    RepairService repair(dbClient);
    auto repair_data = repair.getMyRepairs(user_id);

        // Формируем JSON-ответ
        Json::Value jsonNewsArray;
        for (auto news_current : repair_data)
        {
            Json::Value jsonNewsItem;
            jsonNewsItem["id"] = news_current.getId();
            jsonNewsItem["type"] = news_current.getType();
            jsonNewsItem["body"] = news_current.getBody();
            jsonNewsItem["date"] = news_current.getDate();
            jsonNewsItem["room"] = news_current.getRoom();
            jsonNewsItem["user_id"] = news_current.getUserId();
            jsonNewsItem["activity"] = news_current.getActivity();
            jsonNewsItem["repairman_id"] = news_current.getRepairmanId();
            
            // Добавляем массив изображений
            Json::Value jsonImages(Json::arrayValue);
            for (const auto& image_path : news_current.getRepairPaths()) 
            {
                jsonImages.append(image_path);
            }
            jsonNewsItem["repair_path"] = jsonImages;
            jsonNewsArray.append(jsonNewsItem);
        }

        // 4. Создаем и настраиваем ответ
        auto resp = HttpResponse::newHttpJsonResponse(jsonNewsArray);
        resp->setStatusCode(k200OK);
        callback(resp);
}

void RepairController::activateRepair(const HttpRequestPtr& req,
                            std::function<void(const HttpResponsePtr&)>&& callback)
{
    LOG_ERROR << "Зашли в activateRepair";
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decoded = jwt::decode<traits>(token);
    if (!Headerhelper::verifyToken(decoded)) 
    {
        Headerhelper::responseCheckToken(callback);
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
    int repair_id = json->get("repair_id", "").asInt();
    bool activity = json->get("activity", "").asBool();
    int repairman_id = json->get("user_id", "").asInt();
    LOG_ERROR << "Извлекли данные"<<repairman_id;
    
    if (activity != true && activity != false)
    {
        auto resp = HttpResponse::newHttpResponse();
        resp->setStatusCode(k400BadRequest);
        callback(resp);
    }
    

    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    RepairService repair(dbClient);
    auto success = repair.changeActivateRepair(repair_id,
                                               activity,
                                               repairman_id);

    if (success)
    {
        // Простой JSON ответ с успехом
        Json::Value jsonResponse;
        jsonResponse["success"] = true;
        jsonResponse["message"] = "Repair activity updated successfully";
        jsonResponse["id"] = repair_id;
        jsonResponse["new_activity"] = activity; // или !activity если инвертируется
        
        auto resp = HttpResponse::newHttpJsonResponse(jsonResponse);
        resp->setStatusCode(k200OK); // 200 OK - успешное обновление
        callback(resp);
    }
    else 
    {
        // JSON ответ с ошибкой
        Json::Value jsonResponse;
        jsonResponse["success"] = false;
        jsonResponse["error"] = "Failed to update repair activity";
        jsonResponse["id"] = repair_id;
        
        auto resp = HttpResponse::newHttpJsonResponse(jsonResponse);
        resp->setStatusCode(k400BadRequest);
        callback(resp);
    }
}


void RepairController::deleteRepair(const HttpRequestPtr& req,
                                    std::function<void(const HttpResponsePtr&)>&& callback, 
                                    int id_repair, int id_user)
{
    LOG_ERROR << "Зашли в deleteRepair";
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decoded = jwt::decode<traits>(token);
    
    if (!Headerhelper::verifyToken(decoded))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }
    
    if (!Headerhelper::checkRoles(decoded, "repair_write") && id_user != decoded.get_payload_claim("Id").as_integer())
    {
        throw std::runtime_error("Not rights Role - Repair_write");
    }

    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    RepairService repair(dbClient);
    
    bool result = repair.deleteRepair(id_repair);
    
    if (!result)
    {
        auto resp = HttpResponse::newHttpResponse();
        resp->setStatusCode(k404NotFound);
        callback(resp);
        return;
    }
    auto resp = HttpResponse::newHttpResponse();
    // 3. Возвращаем 204 No Content
    resp->setStatusCode(k204NoContent);
    callback(resp);
}