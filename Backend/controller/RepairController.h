#pragma once
#include <HttpController.h>
#include <DbClient.h>
#include <drogon.h>
#include <json/json.h>
#include "Repair.h"
#include "Util.h"
#include "S3Service.h"
#include "RepairService.h"
#include <string>
#include <filesystem>
#include <jwt-cpp/jwt.h>
#include "traits.h"
using traits = jwt::traits::open_source_parsers_jsoncpp;

using namespace drogon;

class RepairController : public HttpController<RepairController>
{
public:
    METHOD_LIST_BEGIN
        ADD_METHOD_TO(RepairController::postRepair, "/repair", Post);
        ADD_METHOD_TO(RepairController::getMyRepairs, "/myrepair/{}", Get);
        ADD_METHOD_TO(RepairController::activateRepair, "/activaterepair", Post);
        ADD_METHOD_TO(RepairController::endingRepair, "/endingrepair", Post);
        ADD_METHOD_TO(RepairController::deleteRepair, "/repair/{}/{}", Delete);

    METHOD_LIST_END

    void postRepair(const HttpRequestPtr& req,
                            std::function<void(const HttpResponsePtr&)>&& callback);

    void getMyRepairs(const HttpRequestPtr& req,
                            std::function<void(const HttpResponsePtr&)>&& callback,
                            std::string user_type);

    void deleteRepair(const HttpRequestPtr& req,
                            std::function<void(const HttpResponsePtr&)>&& callback, 
                            int id_repair, int id_user);

    void activateRepair(const HttpRequestPtr& req,
                            std::function<void(const HttpResponsePtr&)>&& callback);
    
    void endingRepair(const HttpRequestPtr& req,
                            std::function<void(const HttpResponsePtr&)>&& callback);
};