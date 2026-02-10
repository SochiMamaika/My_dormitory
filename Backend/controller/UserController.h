#pragma once
#include <HttpController.h>
#include "UserService.h"
#include "User.h"
#include "UserRepository.h"
#include <DbClient.h>
#include <drogon.h>
#include <json/json.h>
#include "Util.h"

using namespace drogon;

class UserController : public HttpController<UserController>
{
public:
    METHOD_LIST_BEGIN
    ADD_METHOD_TO(UserController::login, "/login", Post);
    ADD_METHOD_TO(UserController::registerUser, "/register", Post);
    ADD_METHOD_TO(UserController::refresh, "/refresh", Post);
    ADD_METHOD_TO(UserController::getUsers, "/users", Get);
    ADD_METHOD_TO(UserController::getUser, "/users/{}", Get);
    ADD_METHOD_TO(UserController::deleteUser, "/users/{}", Delete);
    ADD_METHOD_TO(UserController::addRole, "/user/{}",Put);
    ADD_METHOD_TO(UserController::deleteRole, "/user/{}",Delete);
    METHOD_LIST_END

    void login(const HttpRequestPtr& req,
               std::function<void(const HttpResponsePtr&)>&& callback);

    void refresh(const HttpRequestPtr& req,
                 std::function<void(const HttpResponsePtr&)>&& callback);

    void registerUser(const HttpRequestPtr& req,
                      std::function<void(const HttpResponsePtr&)>&& callback);

    void getUsers(const HttpRequestPtr& req,
                  std::function<void(const HttpResponsePtr&)>&& callback);
    
    void getUser(const HttpRequestPtr& req,
                 std::function<void(const HttpResponsePtr&)>&& callback, int userId);
    
    void deleteUser(const HttpRequestPtr& req,
                    std::function<void(const HttpResponsePtr&)>&& callback, int userId);

    void addRole(const HttpRequestPtr& req,
                 std::function<void(const HttpResponsePtr&)>&& callback, int user_id);

    void deleteRole(const HttpRequestPtr& req,
                    std::function<void(const HttpResponsePtr&)>&& callback, int user_id);
};
