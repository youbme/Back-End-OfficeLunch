package com.officelunch.controller;

import com.officelunch.model.Availability;
import com.officelunch.model.FeedBack;
import com.officelunch.model.Role;
import com.officelunch.model.User;
import com.officelunch.repositories.AvailabilityRepo;
import com.officelunch.repositories.UserRepositories;
import com.officelunch.security.JavaTokenUtil;
import com.officelunch.security.UserSpringDetails;
import com.officelunch.service.AvailabilityService;
import com.officelunch.service.UserService;
import com.officelunch.service.serviceImpl.FeedBackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/officeLunch/employees")
@CrossOrigin(origins = "http://192.168.1.71:3000", allowedHeaders = "*")
//@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
public class UserController {
    @Autowired
    JavaTokenUtil jwt;
    @Autowired
    UserRepositories userRepositories;
    @Autowired
    private UserService userService;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    AvailabilityRepo availabilityRepo;
    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private FeedBackService feedBackService;
//    @Autowired
//    private ConsequentDaysValidator validator;

    @Autowired
    BCryptPasswordEncoder encoder;

//    @Autowired
//    PasswordEncoder passwordEncoder;

//    @PostMapping("/register")
//    public TokenResponse postUser(@RequestBody User user) {
//        System.out.println(user.getUsername());
//        if (userService.saveUser(user)!=null){
//            if (userServiceTwo.register(user)!=null){
//                return new ResponseEntity<>(user, HttpStatus.CREATED);
//
//            }else {
//                return ResponseEntity.badRequest().body("Duplicate Entry");
//            }
//        HttpStatus created = HttpStatus.CREATED;
//        return new ResponseEntity<>(HttpStatus.CREATED);
//    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        Set<String> roles = authentication.getAuthorities().stream()
//                .map(r -> r.getAuthority()).collect(Collectors.toSet());

//        System.out.println(roles);
        if (userRepositories.existsByUsername(user.getUsername().toLowerCase())) {
            return ResponseEntity.badRequest().body("Duplicate entry of Username");
        }
        if (userRepositories.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("Duplicate entry of Email");
        }
        if (Pattern.compile("^[A-Za-z0-9._%+-]+@(accessonline\\.io|gmail\\.com)$").matcher(user.getEmail()).find()) {
            if (user.getPassword().equals(user.getConfirmPass())) {
                userService.saveUser(user);
            } else {
                return ResponseEntity.badRequest().body("Password Do not match");
            }
            return ResponseEntity.ok().body("Register success");
        } else {
            return ResponseEntity.badRequest().body("Error: Email must contain  domain name '@accessonline.io' and '@gmail.com' ");
        }
    }
//
//        }
//    @PostMapping("/login")
//    public ResponseEntity<?> loginUser(@RequestBody User user, HttpSession session) {
//        if (user.getUsername() == null || user.getPassword() == null) {
//            return new ResponseEntity<>("Username or Password is Empty", HttpStatus.EXPECTATION_FAILED);
//        }
//        if (!userRepositories.existsByUsername(user.getUsername())){
//            return ResponseEntity.badRequest().body("Username is not registered. Register first To Login");
//        }
//        User userData = userService.getUserByUserName(user.getUsername(), user.getPassword());
//        if (userData == null) {
//            return new ResponseEntity<>("username or password is invalid", HttpStatus.BAD_GATEWAY);
//        } else {
//           session.setAttribute("username", user.getUsername());
//          System.out.println(session.getAttributeNames()+ "++++");
//            return new ResponseEntity<>(user.getUsername(), HttpStatus.OK);
//
////            return  ResponseEntity.ok().headers();
//        }
//    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUserAndGenerateToken(@RequestBody User user, Principal principal) {
        User usr;

            if (principal != null) {
                usr = userRepositories.findByUsername(principal.getName());

            } else {
                usr = userRepositories.findByUsername(user.getUsername());
            }

        String role=usr.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.joining(", "));

        if(role.equals("ROLE_ADMIN")||usr.isStat()){
            if (encoder.matches(user.getPassword(), usr.getPassword())) {
                try {
                    Authentication authentication = authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(user.getUsername().toLowerCase(), user.getPassword()));
                    if (authentication.isAuthenticated()) {

//                    TokenResponse tok = new TokenResponse();
//                tok.setToken(jwt.generateToken(user.getUsername()));
                        String tokens = jwt.generateToken(user.getUsername().toLowerCase(), authentication.getAuthorities());
                        return ResponseEntity.ok().body(tokens);
                    } else {
                        throw new UsernameNotFoundException(user.getUsername());
                    }
                } catch (AuthenticationException e) {
                    e.printStackTrace();
                }

            }
        }else {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(user.getUsername().toLowerCase(), user.getPassword()));
                if (authentication.isAuthenticated()) {
                    Boolean stat =usr.isStat();
                    String tokens = jwt.generateToken(user.getUsername().toLowerCase(), authentication.getAuthorities());
                    return ResponseEntity.ok().body(stat);
                } else {
                    throw new UsernameNotFoundException(user.getUsername());
                }
            } catch (AuthenticationException e) {
                e.printStackTrace();
            }
        }



        return ResponseEntity.badRequest().body("Password Do not Match");
    }

    //    @PostMapping("/enroll")
//    public ResponseEntity<?> enrollForFood(@RequestBody Availability availability) {
////        HttpSession session = request.getSession();
////        String name = (String) session.getAttribute("username");
//        String name = availability.getUsername();
//        if (name!=null && name.equals(availability.getUsername())) {
//            User user = userRepositories.findByUsername(name);
//            availability.setUser(user);
//            availability.setAttendance("Present");
//            return ResponseEntity.ok().body(availabilityService.saveEmployeeStatus(availability, user.getId()));
//        } else {
//            if(!userRepositories.existsByUsername(availability.getUsername())){
//                return ResponseEntity.badRequest().body("user is not registered. Register First to enroll for food");
//            }else
//            {
//                return ResponseEntity.badRequest().body("provided username is not logged in. Log in First To order food");
//            }
//        }
//    }
//    @PostMapping("/enroll")
//    public ResponseEntity<?> enrollForFood(@RequestBody Availability availability) {
////        HttpSession session = request.getSession();
////        String name = (String) session.getAttribute("username");
////            jwt.getUsernameFromToken()
//             user = userRepositories.findByUsername(availability.getUsername());
//            availability.setUser(user);
//            availability.setAttendance("Present");
//            return ResponseEntity.ok().body(availabilityService.saveEmployeeStatus(availability, user.getId()));
////        } else {
////            if(!userRepositories.existsByUsername(availability.getUsername())){
////                return ResponseEntity.badRequest().body("user is not registered. Register First to enroll for food");
////            }else
////            {
////                return ResponseEntity.badRequest().body("provided username is not logged in. Log in First To order food");
////            }
////        }
//    }

    @GetMapping("/test")
    public ResponseEntity<?> listOfAllFood() {
//        List<Object> allList= new ArrayList<>();
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode user = objectMapper.readTree(availabilityRepo.listOfFoodTypes("2023-08-28"));
//        JsonNode count = objectMapper.readTree(availabilityRepo.countAllFoodType("2023-08-28"));
//        Object userInJson = objectMapper.treeToValue(user, Object.class);
//        Object countInJson = objectMapper.treeToValue(count, Object.class);
//        allList.add(userInJson);
//        allList.add(countInJson);
        return ResponseEntity.ok().body(availabilityRepo.countAllFoodType(LocalDate.now().toString()));
    }


    @GetMapping("/getAll")
    public ResponseEntity<?> getAllCountOfVegAndNonVeg() {
        String today = LocalDate.now().toString();
        return ResponseEntity.ok().body(availabilityRepo.countAllFoodType(today));
    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<?> getAllCountOfVegAndNonVegUsers() {
        String today = LocalDate.now().toString();
        return ResponseEntity.ok().body(availabilityRepo.listOfFoodTypes(today));
    }

    @PostMapping("/enroll")

    public ResponseEntity<?> post(@RequestBody Availability availability) {

        UserSpringDetails user = (UserSpringDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String name = user.getUsername().toLowerCase();
        User usr = userRepositories.findByUsername(name);
        LocalDate today = LocalDate.now();
        System.out.println(today);
        availability.setUser(usr);
        System.out.println(availabilityRepo.existByDateAndUserId(today.toString(), usr.getId()));
        if (availabilityRepo.existByDateAndUserId(today.toString(), usr.getId()) != null) {
            return ResponseEntity.badRequest().body("You have already inserted for today");
        } else {
            availability.setDate(today);
            return ResponseEntity.ok().body(availabilityService.saveEmployeeStatus(availability));
        }
    }

    @PostMapping("/pwReset")
//    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> resetPassword(@RequestBody User user) {
        if (user.getPassword().equals(user.getConfirmPass())) {
            return new ResponseEntity<>(userService.resetUserPassword(user), HttpStatus.OK);
        } else {
            return ResponseEntity.badRequest().body("Password do not match ");
        }
    }
    @PostMapping("/changePass")
//    @PreAuthorize("hasAnyAuthority('ROLE_USER')")
    public ResponseEntity<?> resetPassword(@RequestBody Map<Object,String> userData,Principal principal) {

        User usr = userRepositories.findByUsername(principal.getName().toLowerCase());
        System.out.println(usr.getUsername());
        if(encoder.matches(userData.get("oldPass"), usr.getPassword())){
            if (userData.get("newPass").equals(userData.get("confirmPass"))) {
                return new ResponseEntity<>(userService.changeUserPassword(userData.get("newPass"),usr), HttpStatus.OK);
            } else {
                return ResponseEntity.badRequest().body("Password do not match ");
            }
        }else {
            return ResponseEntity.badRequest().body("old Password do not match");
        }

    }

    @PostMapping("/logout")
    public String logout() {
        return "user logged out successfully";
    }


    @PostMapping("/range")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> countOfMonth(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok().body(availabilityService.countRangeTotal(payload.get("date1").toString(), payload.get("date2").toString()));
    }

    @PostMapping("/single")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> countOfDay(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok().body(availabilityService.countDailyTotal(payload.get("date").toString()));
    }


    @PostMapping("/feedback")
    public ResponseEntity<?> receiveFeedBack(@RequestBody FeedBack feedBack) {
        if (feedBack.getContent().trim().split("\\s+").length > 200) {
            return ResponseEntity.badRequest().body("Too loong Content");
        } else {
            return ResponseEntity.ok().body(feedBackService.saveFeedBack(feedBack));
        }
    }


    @GetMapping("/feedbackToAdmin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> showFeedBack() {
        return ResponseEntity.ok().body(feedBackService.showAllFeedback());
    }

    @DeleteMapping("/feedBackDelete/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> showFeedBack(@PathVariable int id) {
        feedBackService.deleteFeedback(id);
        return ResponseEntity.ok().body("deleted Successfully");
    }
}

