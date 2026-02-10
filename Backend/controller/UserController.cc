#include "UserController.h"
#include <string>
#include "traits.h"
using traits = jwt::traits::open_source_parsers_jsoncpp;

void UserController::login(const HttpRequestPtr& req,
                           std::function<void(const HttpResponsePtr&)>&& callback) 
{
    // 1. Получаем JSON из запроса
    auto json = req->getJsonObject();
    if (!json) 
    {
        Headerhelper::responseCheckJson(callback);
        return;
    }

    // 2. Извлекаем номер и пароль
    std::string phone_numberWithPlus7 = json->get("phone_number", "").asString();
    std::string password = json->get("password", "").asString();

    if (phone_numberWithPlus7[0] == '8')
    {
        phone_numberWithPlus7 = "+7" + phone_numberWithPlus7.substr(1);
    }
    std::string phone_number = phone_numberWithPlus7;
    if (phone_number.empty() || password.empty() || phone_number.length() !=12) 
    {
        Json::Value error;
        error["error"] = "Phone number and password are Unright";
        error["code"] = 400;
        
        auto resp = HttpResponse::newHttpJsonResponse(error);
        resp->setStatusCode(k400BadRequest);
        callback(resp);
        return;
    }
    LOG_ERROR << "Phone number: " << phone_number;

    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();

    // 4. Создаём сервис
    UserService userService(dbClient);

    // 5. Вызываем метод логина
    auto tokens = userService.login(phone_number, 
                                    password);

    if (tokens.size() != 3 || tokens.empty()) 
    {
        auto resp = HttpResponse::newHttpResponse();
        resp->setStatusCode(k401Unauthorized);
        callback(resp);
        return;
    }

    std::string access_token = *tokens.begin();
    std::string refresh_token = *std::next(tokens.begin());
    std::string userType = *std::next(tokens.begin(), 2);  // третий

    // 6. Формируем JSON-ответ
    Json::Value ret;
    ret["access_token"] = access_token;
    ret["refresh_token"] = refresh_token;
    ret["userType"] = userType;
    ret["token_type"] = "bearer";
    ret["status"] = "success";
    
    // 7. Отправляем ответ
    auto resp = HttpResponse::newHttpJsonResponse(ret);
    resp->setStatusCode(k200OK);
    callback(resp);
}

void UserController::refresh(const HttpRequestPtr& req,
                 std::function<void(const HttpResponsePtr&)>&& callback)
{
    LOG_ERROR << "Зашли в метод refresh token";
    auto json = req->getJsonObject();
    if (!json || !json->isMember("refresh_token"))
    {
        auto resp = HttpResponse::newHttpJsonResponse(Json::Value("Missing refresh_token"));
        resp->setStatusCode(k400BadRequest);
        callback(resp);
        return;
    }

    std::string refreshToken = (*json)["refresh_token"].asString();

    try
    {
        // 3. Получаем подключение к БД
        auto dbClient = drogon::app().getDbClient();
        UserService userservice(dbClient);
        auto tokens = userservice.refreshTokens(refreshToken);

        Json::Value ret;
        ret["access_token"] = *tokens.begin();
        ret["refresh_token"] = *std::next(tokens.begin());
        auto resp = HttpResponse::newHttpJsonResponse(ret);
        resp->setStatusCode(k200OK);
        callback(resp);
    }
    catch (const std::exception &e)
    {
        LOG_ERROR << e.what();
        auto resp = HttpResponse::newHttpJsonResponse(Json::Value(e.what()));
        resp->setStatusCode(k401Unauthorized);
        callback(resp);
    }
}

void UserController::registerUser(const HttpRequestPtr& req,
                                  std::function<void(const HttpResponsePtr&)>&& callback)
{
    LOG_ERROR << "зашли в метод регистрации";
    // 1. Получаем JSON из запроса
    auto json = req->getJsonObject();
    if (!json) 
    {
        Headerhelper::responseCheckJson(callback);
        return;
    }

    // 2. Извлекаем даные
    std::string phone_number = json->get("phone_number", "").asString();
    std::string password = json->get("password", "").asString();
    std::string name = json->get("name", "").asString();
    std::string last_name = json->get("last_name", "").asString();
    std::string surname = json->get("surname", "").asString();
    std::string typeName = json->get("typeName", "").asString();
    LOG_ERROR << "Извлекли данные";
    std::list<std::string> document_paths;
    if (!json->isMember("document_path") || !(*json)["document_path"].isArray()) 
    {
        Headerhelper::responseCheckJson(callback);
        return;
    }
    LOG_ERROR << "файл прошел проверку";

    const Json::Value& imagesArray = (*json)["document_path"];
        for (const auto& doc : imagesArray) 
        {
            std::string path = doc.asString();
            if (!path.empty()) 
            {
                document_paths.push_back(path); // добавляем в list
            }
        }

        LOG_ERROR << "Извлекли данные из файла";

    if (password.size() < 5)
    {
        Json::Value error;
        error["error"]  = "Password should be > 4 symbol";
        error["status"] = "error";
        auto resp = HttpResponse::newHttpJsonResponse(error);
        resp->setStatusCode(k400BadRequest);
        callback(resp);
        return;
    }

    LOG_ERROR << "проверили пароль на длину";

    if (phone_number.empty() || password.empty() || name.empty() || last_name.empty() || surname.empty() || document_paths.empty()) 
    {
        Json::Value error;
        error["error"]  = "All fields are required";
        error["status"] = "error";
        auto resp = HttpResponse::newHttpJsonResponse(error);
        resp->setStatusCode(k400BadRequest);
        callback(resp);
        return;
    }

    // 3. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();

    // 4. Создаём сервис
    UserService userService(dbClient);

    if (userService.checkUserExists(phone_number)) 
    {
        Json::Value error;
        error["error"] = "User with phone number " + phone_number + " already exists";
        error["code"] = 409;
        auto resp = HttpResponse::newHttpJsonResponse(error);
        resp->setStatusCode(k409Conflict);
        callback(resp);
        return;
    }
    LOG_ERROR << "Phone number: " << phone_number;

    // 5. Вызываем метод регистрации
    auto users = userService.registerUser(phone_number,
                                          password,
                                          name,
                                          last_name,
                                          surname,
                                          document_paths,
                                          typeName);

    Json::Value message;
    message["message"] = "User Created!";
    auto resp = HttpResponse::newHttpJsonResponse(message);
    resp->setStatusCode(k201Created);
    callback(resp);
}

void UserController::getUsers(const HttpRequestPtr& req,
                              std::function<void(const HttpResponsePtr&)>&& callback)
{
    auto token = Headerhelper::getTokenFromHeaders(req);
    auto decoded = jwt::decode<traits>(token);
    
    if (!Headerhelper::verifyToken(decoded))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }

    if (!Headerhelper::checkRoles(decoded, "user_read"))
    {
        
        Headerhelper::responseCheckRoles(callback);
        return;
    }

    // 1. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    // 2. Создаём сервис
    UserService userService(dbClient);
    auto users = userService.getUsers();

    // 3. Формируем JSON-ответ
    Json::Value jsonUsers;
    for (auto user : users) 
    {
        Json::Value jsonUser;
        jsonUser["id"] = user.getId();
        jsonUser["phone_number"] = user.getPhoneNumber();
        jsonUser["name"] = user.getName();
        jsonUser["last_name"] = user.getLastName();
        jsonUser["surname"] = user.getSurname();
        
        Json::Value jsonFiles(Json::arrayValue);
        for (const auto& document_path : user.getDocument()) 
        {
            jsonFiles.append(document_path);
        }
        jsonUser["document_path"] = jsonFiles;

        Json::Value jsonRoles(Json::arrayValue);
        for (const auto& role : user.getRoles()) 
        {
            jsonRoles.append(role);
        }
        jsonUser["roles"] = jsonRoles;
        
        
        jsonUsers.append(jsonUser);
    }

    // 4. Создаем и настраиваем ответ
    auto resp = HttpResponse::newHttpJsonResponse(jsonUsers);
    resp->setStatusCode(k200OK);
    callback(resp);
}

void UserController::getUser(const HttpRequestPtr& req,
                             std::function<void(const HttpResponsePtr&)>&& callback, 
                             int user_id)
{
    // 6. Получение данных пользователя
    auto dbClient = drogon::app().getDbClient();
    UserService userService(dbClient);
    UserData user;
    std::string token = Headerhelper::getTokenFromHeaders(req);
    auto decoded = jwt::decode<traits>(token);
    // Проверка id доступа
    auto idClaim = decoded.get_payload_claim("Id").as_integer();
    
    if (!Headerhelper::verifyToken(decoded))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }

    if (!Headerhelper::checkRoles(decoded, "user_read") && user_id != idClaim)
    {
        Headerhelper::responseCheckRoles(callback);
        return;
    }
    try
    {
        user = userService.getUser(user_id);
    }
    catch(const std::exception& e)
    {
        std::string err = "User not Found";
        auto resp = HttpResponse::newHttpJsonResponse(err);
        resp->setStatusCode(k400BadRequest);
        callback(resp);
    }

    // 7. Формирование безопасного ответа (без пароля)
    Json::Value jsonUser;
    jsonUser["id"] = user.getId();
    jsonUser["phone_number"] = user.getPhoneNumber();
    jsonUser["name"] = user.getName();
    jsonUser["last_name"] = user.getLastName();
    jsonUser["surname"] = user.getSurname();
    Json::Value rolesArray(Json::arrayValue); // Создаем JSON-массив
    Json::Value docArray(Json::arrayValue); // Создаем JSON-массив

    for (const auto& role : user.getRoles())
    {
        rolesArray.append(role); // Добавляем каждую роль в массив
    }
    jsonUser["roles"] = rolesArray; // Добавляем массив в основной объект

    for (const auto& doc : user.getDocument())
    {
        docArray.append(doc);
    }
    jsonUser["document"] = docArray;
    auto resp = HttpResponse::newHttpJsonResponse(jsonUser);
    resp->setStatusCode(k200OK);
    callback(resp);
}

void UserController::deleteUser(const HttpRequestPtr& req,
                                std::function<void(const HttpResponsePtr&)>&& callback, 
                                int userId)
{
    std::string token = Headerhelper::getTokenFromHeaders(req);
    // 3. Декодирование JWT с улучшенной обработкой ошибок
    auto decoded = jwt::decode<traits>(token);
    //получение ID
    auto idClaim = decoded.get_payload_claim("Id").as_integer();
    
    if (!Headerhelper::verifyToken(decoded))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }

    if (!Headerhelper::checkRoles(decoded, "user_write") && userId != idClaim)
    {
        Headerhelper::responseCheckRoles(callback);
        return;
    }

    // 6. Получение данных пользователя
    auto dbClient = drogon::app().getDbClient();
    UserService userService(dbClient);
    
    bool result = userService.deleteUser(userId);

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

void UserController::addRole(const HttpRequestPtr& req,
                             std::function<void(const HttpResponsePtr&)>&& callback, 
                             int user_id)
{
    LOG_ERROR << "addRole";
    // 1. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    // 2. Создаём сервис
    UserService userService(dbClient);
    std::string token = Headerhelper::getTokenFromHeaders(req);
    // 3. Декодирование JWT с улучшенной обработкой ошибок
    auto decoded = jwt::decode<traits>(token);
    
    if (!Headerhelper::verifyToken(decoded))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }

    if (!Headerhelper::checkRoles(decoded, "role_write"))
    {
        Headerhelper::responseCheckRoles(callback);
        return;
    }

    userService.addRole(user_id); 
    
    // 3. Возвращаем 201
    auto resp = HttpResponse::newHttpResponse();
    resp->setStatusCode(k201Created);
    callback(resp);
}

void UserController::deleteRole(const HttpRequestPtr& req,
                                std::function<void(const HttpResponsePtr&)>&& callback, 
                                int user_id)
{
    LOG_ERROR << "deleteRole";
    // 1. Получаем подключение к БД
    auto dbClient = drogon::app().getDbClient();
    // 2. Создаём сервис
    UserService userService(dbClient);
    std::string token = Headerhelper::getTokenFromHeaders(req);
    // 3. Декодирование JWT с улучшенной обработкой ошибок
    auto decoded = jwt::decode<traits>(token);
    
    if (!Headerhelper::verifyToken(decoded))
    {
        Headerhelper::responseCheckToken(callback);
        return;
    }
    
    if (!Headerhelper::checkRoles(decoded, "role_write"))
    {
        Headerhelper::responseCheckRoles(callback);
        return;
    }
    
    bool result = userService.deleteRole(user_id); 
    
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