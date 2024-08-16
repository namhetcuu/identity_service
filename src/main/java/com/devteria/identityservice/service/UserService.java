package com.devteria.identityservice.service;

import com.devteria.identityservice.dto.request.UserCreationRequest;
import com.devteria.identityservice.dto.request.UserUpdateRequest;
import com.devteria.identityservice.entity.User;
import com.devteria.identityservice.exception.AppException;
import com.devteria.identityservice.exception.ErrorCode;
import com.devteria.identityservice.mapper.UserMapper;
import com.devteria.identityservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMapper userMapper;

    public User createUser(UserCreationRequest request){

        if(userRepository.existsByUsername(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);

        User user = userMapper.toUser(request);

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        return userRepository.save(user);

    }

    public List<User> getUsers(){

        return userRepository.findAll();

    }

    public User getUser(String id){

        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

    }

    public User updateUser(String userId, UserUpdateRequest request){

        User user = getUser(userId);
        userMapper.updateUser(user,request);

        return userRepository.save(user);

    }

    public void deleteUser(String userId){

        userRepository.deleteById(userId);

    }
}
