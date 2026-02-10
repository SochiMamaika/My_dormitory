#include "UserRepository.h"
// Реализация метода создания пользователя
UserData UserRepository::createUser(const std::string &phone_number, 
                                    const std::string &passwordHash,
                                    const std::string &name,
                                    const std::string &last_name,
                                    const std::string &surname,
                                    const std::list<std::string> &document)
{
    auto trans = db_->newTransaction();
    // 1. Создаём пользователя
    auto result = trans->execSqlSync
    (
        "INSERT INTO users (phone_number, password, name, last_name, surname) "
        "VALUES ($1, $2, $3, $4, $5) "
        "RETURNING id, phone_number, password, name, last_name, surname",
        phone_number, passwordHash, name, last_name, surname
    );
    UserData user;
    user.fromDb(result[0]);
    int user_id = user.getId();
    
    if (!document.empty())
    {
        for (auto doc : document)
        {
            auto result_2 = trans->execSqlSync
            (
                "INSERT INTO user_file (user_id, file_path) "
                "VALUES ($1, $2) ",
                user_id, doc
            );
        }
    }
    user.setDocuments(document);
    // 2. Назначаем роли
    std::vector<std::string> roleNames = {"news_read", "user_read", "tutor_read", "file_read", "wash_machine_read, repair_read"};

    for (const auto &roleName : roleNames)
    {
        auto roleResult = trans->execSqlSync
        (
            "SELECT id FROM roles WHERE role_type = $1 ",
            roleName
        );
        if (roleResult.empty())
        {
            LOG_WARN << "Роль " << roleName << " не найдена!";
            continue;
        }
        
        int roleId = roleResult[0]["id"].as<int>();
        // 3. Добавляем связь user-role
        trans->execSqlSync
        (
            "INSERT INTO users_roles (user_id, role_id) VALUES ($1, $2)",
            user.getId(), roleId
        );
    }
    std::list<std::string> roles(roleNames.begin(), roleNames.end());
    user.setRoles(roles);
    return user;
}


UserData UserRepository::createRepairMan(const std::string &phone_number, 
                                         const std::string &passwordHash,
                                         const std::string &name,
                                         const std::string &last_name,
                                         const std::string &surname,
                                         const std::list<std::string> &document)
{
    auto trans = db_->newTransaction();
    // 1. Создаём пользователя
    auto result = trans->execSqlSync
    (
        "INSERT INTO repairman (phone_number, password, name, last_name, surname) " //таблицы нету
        "VALUES ($1, $2, $3, $4, $5) "
        "RETURNING id, phone_number, password, name, last_name, surname",
        phone_number, passwordHash, name, last_name, surname
    );
    UserData repair;
    repair.fromDb(result[0]);
    int user_id = repair.getId();
    
    if (!document.empty())
    {
        for (auto doc : document)
        {
            auto result_2 = trans->execSqlSync
            (
                "INSERT INTO repairman_file (repairman_id, file_path) " //таблицы нету
                "VALUES ($1, $2) ",
                user_id, doc
            );
        }
    }
    repair.setDocuments(document);
    // 2. Назначаем роли
    std::vector<std::string> roleNames = {"news_read", "user_read", "tutor_read", "file_read", "wash_machine_read, repair_read"};

    for (const auto &roleName : roleNames)
    {
        auto roleResult = trans->execSqlSync
        (
            "SELECT id FROM roles WHERE role_type = $1 ",
            roleName
        );
        if (roleResult.empty())
        {
            LOG_WARN << "Роль " << roleName << " не найдена!";
            continue;
        }
        
        int roleId = roleResult[0]["id"].as<int>();
        // 3. Добавляем связь user-role
        trans->execSqlSync
        (
            "INSERT INTO repairman_roles (repairman_id, role_id) VALUES ($1, $2)",
            repair.getId(), roleId
        );
    }
    std::list<std::string> roles(roleNames.begin(), roleNames.end());
    repair.setRoles(roles);
    return repair;
}


bool UserRepository::checkUserExists(const std::string &phone_number)
{
    auto result = db_->execSqlSync
    (
        "SELECT * FROM users "
        "WHERE phone_number = $1", 
        phone_number
    );
    return !result.empty();
}

UserData UserRepository::getUserByPhone(const std::string &phone_number) 
{
    {
        auto tran = db_->newTransaction();
        UserData user;
        auto result = tran->execSqlSync
        (
            "SELECT * FROM users u "
            "WHERE u.phone_number = $1 ", 
            phone_number
        );

        if (!result.empty()) 
        {

            user.fromDb(result[0]);
            auto result_2 = tran->execSqlSync
            (
                "SELECT r.role_type FROM users u " 
                "JOIN users_roles u_r ON u.id = u_r.user_id "
                "JOIN roles r ON u_r.role_id = r.id "
                "WHERE u.phone_number = $1", 
                phone_number
            );
            
            std::list<std::string> role_type;
            for (int i = 0; i < result_2.size(); i++)
            {
                role_type.push_back(result_2[i]["role_type"].as<std::string>());
            }
            user.setRoles(role_type);

            auto result_3 = tran->execSqlSync
            (
                "SELECT u_f.file_path FROM users u " 
                "JOIN user_file u_f ON u.id = u_f.user_id "
                "WHERE u.phone_number = $1", 
                phone_number
            );

            std::list<std::string> file_path;
            for (int i = 0; i < result_3.size(); i++)
            {
                file_path.push_back(result_3[i]["file_path"].as<std::string>());
            }
            user.setDocuments(file_path);
            user.setTypeName("Студент");
            LOG_ERROR << "type Студент";
            return user;
        }
    }

    {
        auto tran = db_->newTransaction();
        UserData user;
        auto result = tran->execSqlSync
        (
            "SELECT * FROM repairman r "
            "WHERE r.phone_number = $1 ", 
            phone_number
        );

        if (!result.empty()) 
        {

            user.fromDb(result[0]);
            auto result_2 = tran->execSqlSync
            (
                "SELECT r.role_type FROM repairman rm " 
                "JOIN repairman_roles r_r ON rm.id = r_r.repairman_id "
                "JOIN roles r ON r_r.role_id  = r.id "
                "WHERE rm.phone_number = $1", 
                phone_number
            );
            
            std::list<std::string> role_type;
            for (int i = 0; i < result_2.size(); i++)
            {
                role_type.push_back(result_2[i]["role_type"].as<std::string>());
            }
            user.setRoles(role_type);

            auto result_3 = tran->execSqlSync
            (
                "SELECT r_f.file_path FROM repairman rm " 
                "JOIN repairman_file r_f ON rm.id = r_f.repairman_id "
                "WHERE rm.phone_number = $1", 
                phone_number
            );

            std::list<std::string> file_path;
            for (int i = 0; i < result_3.size(); i++)
            {
                file_path.push_back(result_3[i]["file_path"].as<std::string>());
            }
            user.setDocuments(file_path);
            user.setTypeName("Ремонтник");
            LOG_ERROR << "type Ремонтник";
            return user;
        }    
    }
}

std::list<UserData> UserRepository::getUsers() 
{
    auto tran = db_->newTransaction();
    auto result = tran->execSqlSync
    (
        "SELECT * FROM users "
    );
    std::list<UserData> users;
    
    for (int i=0; i < result.size(); i++)
    {
        UserData user;
        user.fromDb(result[i]);
        int user_id = user.getId();
        auto result_2 = tran->execSqlSync
        (
            "SELECT r.role_type FROM users u " 
            "JOIN users_roles u_r ON u.id = u_r.user_id "
            "JOIN roles r ON u_r.role_id = r.id "
            "WHERE u.id = $1", 
            user_id
        );
        std::list<std::string> role_type;
        for (int i = 0; i < result_2.size(); i++)
        {
            role_type.push_back(result_2[i]["role_type"].as<std::string>());
        }
        user.setRoles(role_type);

        auto result_3 = tran->execSqlSync
        (
            "SELECT u_f.file_path FROM users u " 
            "JOIN user_file u_f ON u.id = u_f.user_id "
            "WHERE u.id = $1", 
            user_id
        );
        std::list<std::string> file_path;
        for (int i = 0; i < result_3.size(); i++)
        {
            file_path.push_back(result_3[i]["file_path"].as<std::string>());
        }
        user.setDocuments(file_path);
        users.push_back(user);
    }
    return users;
}

UserData UserRepository::getUser(int id)
{
    {
        auto tran = db_->newTransaction();
        auto result = tran->execSqlSync
        (
            "SELECT * FROM users "
            "WHERE id = $1", 
            id
        );

        if (!result.empty()) 
        {
            UserData user;
            user.fromDb(result[0]);

            auto result_2 = tran->execSqlSync
            (
                "SELECT r.role_type FROM users u " 
                "JOIN users_roles u_r ON u.id = u_r.user_id "
                "JOIN roles r ON u_r.role_id = r.id "
                "WHERE u.id = $1", 
                id
            );

            std::list<std::string> role_type;
            for (int i=0; i < result_2.size(); i++)
            {
                role_type.push_back(result_2[i]["role_type"].as<std::string>());
            }
            user.setRoles(role_type);
            
            
            auto result_3 = tran->execSqlSync
            (
                "SELECT u_f.file_path FROM users u " 
                "JOIN user_file u_f ON u.id = u_f.user_id "
                "WHERE u.id = $1", 
                id
            );
            std::list<std::string> file_path;
            for (int i = 0; i < result_3.size(); i++)
            {
                file_path.push_back(result_3[i]["file_path"].as<std::string>());
            }
            user.setDocuments(file_path);
            
            return user;
        }
    }

    {
        auto tran = db_->newTransaction();
        auto result = tran->execSqlSync
        (
            "SELECT * FROM repairman "
            "WHERE id = $1", 
            id
        );

        if (result.empty()) 
        {
            throw std::runtime_error("User not found");
        }

        UserData user;
        user.fromDb(result[0]);

        auto result_2 = tran->execSqlSync
        (
            "SELECT r.role_type FROM repairman rm " 
            "JOIN repairman_roles r_r ON rm.id = r_r.repairman_id "
            "JOIN roles r ON r_r.role_id = r.id "
            "WHERE rm.id = $1", 
            id
        );

        std::list<std::string> role_type;
        for (int i=0; i < result_2.size(); i++)
        {
            role_type.push_back(result_2[i]["role_type"].as<std::string>());
        }
        user.setRoles(role_type);
        
        
        auto result_3 = tran->execSqlSync
        (
            "SELECT rm_f.file_path FROM repairman rm " 
            "JOIN repairman_file rm_f ON rm.id = rm_f.repairman_id "
            "WHERE rm.id = $1", 
            id
        );
        std::list<std::string> file_path;
        for (int i = 0; i < result_3.size(); i++)
        {
            file_path.push_back(result_3[i]["file_path"].as<std::string>());
        }
        user.setDocuments(file_path);
        
        return user;
    }
}

bool UserRepository::deleteUser(int id)
{
    auto tran = db_->newTransaction();
    //для начала нужно удалить фотку из minio
    S3Service s3service("mydormitory");
    // 1. Получаем все изображения для удаления из MinIO
    auto files_result = tran->execSqlSync
    (
        "SELECT file_path FROM user_file " 
        "WHERE user_id = $1",
        id
    );
    
    for (auto num : files_result)
    {
        std::string file_path = num["file_path"].as<std::string>();
        s3service.deleteFile(file_path);
    }

    tran->execSqlSync
    (
        "DELETE FROM users_roles "
        "WHERE user_id = $1 ", 
        id
    );
    
    tran->execSqlSync
    (
        "DELETE FROM user_file "
        "WHERE user_id = $1 ", 
        id
    );
    
    auto result = tran->execSqlSync
    (
        "Delete FROM users "
        "WHERE id = $1 ", 
        id
    );
    
    if (result.affectedRows() == 0)
    {
        return false;
    }
    return true;
}


void UserRepository::addRole(int user_id) 
{
    auto trans = db_->newTransaction();
    std::vector<std::string> roleNames = {"news_write", "user_write", "tutor_write", "file_write", "wash_machine_write", "repair_write", "role_write"};
    
    for (const auto &roleName : roleNames)
    {
        auto roleResult = trans->execSqlSync
        (
            "SELECT id FROM roles WHERE role_type = $1 ",
            roleName
        );
        if (roleResult.empty())
        {
            LOG_WARN << "Роль " << roleName << " не найдена!";
            continue;
        }
        
        int roleId = roleResult[0]["id"].as<int>();
        auto existingRole = trans->execSqlSync
        (
            "SELECT 1 FROM users_roles WHERE user_id = $1 AND role_id = $2",
            user_id, roleId
        );
        
        if (existingRole.empty())
        {
            trans->execSqlSync
            (
                "INSERT INTO users_roles (user_id, role_id) VALUES ($1, $2)",
                user_id, roleId
            );
        }
    }
}

bool UserRepository::deleteRole(int user_id)
{
    auto trans = db_->newTransaction();
    
    // Удаляем все роли, которые были добавлены через addRole()
    std::vector<std::string> rolesToDelete = {"news_write", "user_write", "tutor_write", "file_write", "wash_machine_write", "repair_write", "role_write"};
    
    for (const auto &roleName : rolesToDelete)
    {
        auto roleResult = trans->execSqlSync
        (
            "SELECT id FROM roles WHERE role_type = $1",
            roleName
        );
        
        if (!roleResult.empty())
        {
            int roleId = roleResult[0]["id"].as<int>();
            trans->execSqlSync
            (
                "DELETE FROM users_roles WHERE user_id = $1 AND role_id = $2",
                user_id, roleId
            );
        }
    }
    return true;
}
