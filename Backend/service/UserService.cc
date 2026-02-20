#include "UserService.h"
#include <vector>
#include <ranges>
#include <json/json.h>
#include "traits.h"
using traits = jwt::traits::open_source_parsers_jsoncpp;

// Конструктор — инициализируем репозиторий
UserService::UserService(const drogon::orm::DbClientPtr& dbClient)
{
    repository = std::make_shared<UserRepository>(dbClient);
}

// Регистрация пользователя
UserData UserService::registerUser(const std::string &phone_number,
                                   const std::string &password,
                                   const std::string &name,
                                   const std::string &last_name,
                                   const std::string &surname,
                                   const std::list<std::string> &document,
                                   const std::string &typeName)
{

    checkData(phone_number, 
              password);

    // Хешируем пароль
    std::string passwordHash = BCrypt::generateHash(password);

    if (typeName == "Ремонтник")
    {
        return repository->createRepairMan(phone_number,
                                           passwordHash, 
                                           name, 
                                           last_name, 
                                           surname, 
                                           document);
    }
    
    return repository->createUser(phone_number,
                                  passwordHash, 
                                  name, 
                                  last_name, 
                                  surname, 
                                  document);
}

std::list<std::string> UserService::login(const std::string &phone_number, 
                                          const std::string &password)
{
    // Получаем пользователя из БД
    UserData user;
    user = repository->getUserByPhone(phone_number);

    std::string db_password_hash = user.getPassword();

    // Проверяем пароль
    if (!BCrypt::validatePassword(password, db_password_hash)) 
    {
        throw std::runtime_error("Invalid password or login");
    }

    // Генерируем JWT токен
    try 
    {
        std::list<std::string> roles = user.getRoles();
        Json::Value jsonRoles;
        for (const auto& role : roles)
        {
            jsonRoles.append(role); // Добавляем каждую роль в массив
        }

        // Access token (5 минут)
        auto access_token = jwt::create<traits>()
        .set_payload_claim("roles", jsonRoles)
        .set_payload_claim("Id", user.getId())
        .set_subject(phone_number)
        .set_type("access")
        .set_expires_at(std::chrono::system_clock::now() + std::chrono::minutes{2})
        .sign(jwt::algorithm::hs256{"my_super_secret_key_bytes_min_wawawawawwawawwawawawawaw"});

        // Refresh token (7 дней)
        auto refresh_token = jwt::create<traits>()
        .set_payload_claim("Id", user.getId())
        .set_subject(phone_number)
        .set_type("refresh")
        .set_expires_at(std::chrono::system_clock::now() + std::chrono::hours{168})
        .sign(jwt::algorithm::hs256{"my_super_secret_key_bytes_min_wawawawawwawawwawawawawaw"});

        // Возвращаем в списке: [access_token, refresh_token]
        std::string userType = user.getTypeName();
        std::list<std::string> tokens;
        tokens.push_back(access_token);
        tokens.push_back(refresh_token);
        tokens.push_back(userType);
        return tokens;
    } 

    catch (const std::exception &e) 
    {
        throw std::runtime_error(std::string("Token generation failed: ") + e.what());
    }
}


std::list<std::string> UserService::refreshTokens(const std::string &refresh_token)
{
    LOG_ERROR << "Зашли в метод генерации токенов";
    try 
    {
        // Декодируем и проверяем refresh token
        auto decoded = jwt::decode<traits>(refresh_token);
        
        if (!Headerhelper::verifyToken(decoded)) 
        {
            throw std::runtime_error("Invalid or expired refresh token");
        }

        if (decoded.get_type() != "refresh") 
        {
            throw std::runtime_error("Not a refresh token");
        }

        // Получаем данные пользователя
        auto userId = decoded.get_payload_claim("Id").as_integer();
        UserData user = repository->getUser(userId);
        
        std::list<std::string> roles = user.getRoles();
        Json::Value jsonRoles;
        for (const auto& role : roles) 
        {
            jsonRoles.append(role);
        }

        // Генерируем новые токены
        auto new_access_token = jwt::create<traits>()
            .set_payload_claim("roles", jsonRoles)
            .set_payload_claim("Id", user.getId())
            .set_subject(user.getPhoneNumber())
            .set_type("access")
            .set_expires_at(std::chrono::system_clock::now() + std::chrono::minutes{2})
            .sign(jwt::algorithm::hs256{"my_super_secret_key_bytes_min_wawawawawwawawwawawawawaw"});
        auto new_refresh_token = jwt::create<traits>()
            .set_payload_claim("Id", user.getId())
            .set_subject(user.getPhoneNumber())
            .set_type("refresh")
            .set_expires_at(std::chrono::system_clock::now() + std::chrono::hours{168})
            .sign(jwt::algorithm::hs256{"my_super_secret_key_bytes_min_wawawawawwawawwawawawawaw"});
        std::list<std::string> tokens;
        tokens.push_back(new_access_token);
        tokens.push_back(new_refresh_token);
        return tokens;
    } 

    catch (const std::exception &e) 
    {
        throw std::runtime_error(std::string("Token refresh failed: ") + e.what());
    }
}


std::list<UserData> UserService::getUsers()
{
    return repository->getUsers();
};

UserData UserService::getUser(int id)
{
    return repository->getUser(id);
}

bool UserService::deleteUser(int id)
{
    return repository->deleteUser(id);
}

bool UserService::checkUserExists(std::string phone_number)
{
    return repository->checkUserExists(phone_number);
}


void UserService::checkData(const std::string &phone_number, 
                            const std::string &password)
{
    if (phone_number.empty() || password.empty()) 
    {
        throw std::runtime_error("Phone number or password are MULL");
    }

    if (phone_number.size() != 11 && phone_number.size() != 12) 
    {
        throw std::runtime_error("Invalid phone number format");
    }
}

void UserService::addRole(int user_id)
{
    return repository->addRole(user_id);
}

bool UserService::deleteRole(int user_id)
{
    return repository->deleteRole(user_id);
}