#include "WashMachineController.h"
void WashMachineController::addWashMachine(const HttpRequestPtr& req,
                                           std::function<void(const HttpResponsePtr&)>&& callback)
{
         std::string token = Headerhelper::getTokenFromHeaders(req);
        auto decode = jwt::decode<traits>(token);
        if (!Headerhelper::verifyToken(decode))
        {
            Headerhelper::responseCheckToken(callback);
            return;
        }

        int user_id = decode.get_payload_claim("Id").as_integer();
        
        if (!Headerhelper::checkRoles(decode, "wash_machine_write"))
        {
            Headerhelper::responseCheckRoles(callback);
            return;
        }
        // Получаем JSON данные
        auto json = req->getJsonObject();
        if (!json) 
        {
            throw std::runtime_error("Invalid JSON");
        }
        
        std::string name = json->get("name", "").asString();
        // 3. Получаем подключение к БД
        auto dbClient = drogon::app().getDbClient();
        WashMachineService washmachine(dbClient);
        washmachine.addWashMachine(name,
                                   user_id);
        
        auto resp = HttpResponse::newHttpResponse();
        resp->setStatusCode(k201Created);
        callback(resp);
}

void WashMachineController::getWashMachines(const HttpRequestPtr& req,
                                            std::function<void(const HttpResponsePtr&)>&& callback)
{
    LOG_ERROR << "Зашли в метод getWashMachines";
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decode = jwt::decode<traits>(token);
    if (!Headerhelper::verifyToken(decode))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }
    LOG_ERROR << "Прошли проверку токена в getWashMachines";
    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    WashMachineService machine (dbClient);
    auto machines = machine.getWashMachines();
    Json::Value respJson;
    for (auto &m : machines)
    {
        Json::Value machineJson;
        machineJson["id"] = m.getId();
        machineJson["name"] = m.getName();
        
        Json::Value reserveArray(Json::arrayValue);
        for (const auto& reserve : m.getReserve()) 
        {
            Json::Value reserveJson;
            reserveJson["id_reserve"] = reserve.getId();
            reserveJson["user_id"] = reserve.getUserId();
            reserveJson["date"] = reserve.getDate();
            reserveJson["start_time"] = reserve.getStartTime();
            reserveJson["duration"] = reserve.getDuration();
            reserveArray.append(reserveJson);
        }
        machineJson["reservations"] = reserveArray;
        respJson.append(machineJson);
    }
    auto resp = HttpResponse::newHttpJsonResponse(respJson);
    resp->setStatusCode(k200OK);
    callback(resp);
}




void WashMachineController::deleteWashMachine(const HttpRequestPtr& req,
                                              std::function<void(const HttpResponsePtr&)>&& callback, 
                                              int id)
{
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decode = jwt::decode<traits>(token);
    if (!Headerhelper::verifyToken(decode))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }
    
    if (!Headerhelper::checkRoles(decode, "wash_machine_write"))
    {
        Headerhelper::responseCheckRoles(callback);
        return;
    }
    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    WashMachineService machine (dbClient);
    auto result = machine.deleteWashMachine(id);
    if (!result)
    {
        // 3. Возвращаем 404
        auto resp = HttpResponse::newHttpResponse();
        resp->setStatusCode(k404NotFound);
        callback(resp);
    }
    // 3. Возвращаем 204 No Content
    auto resp = HttpResponse::newHttpResponse();
    resp->setStatusCode(k204NoContent);
    callback(resp);
}