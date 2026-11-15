package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.dto.StudentDTO;
import com.workstudy.dto.StudentPageQueryDTO;
import com.workstudy.entity.Student;
import com.workstudy.result.PageResult;
import com.workstudy.result.Result;
import com.workstudy.service.StudentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生档案管理
 */
@RestController
@RequestMapping("/student")
@Slf4j
@Tag(name = "学生档案管理接口")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 分页查询学生
     *
     * @param studentPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询学生")
    public Result<PageResult> page(StudentPageQueryDTO studentPageQueryDTO) {
        log.info("分页查询学生：{}", studentPageQueryDTO);
        PageResult pageResult = studentService.pageQuery(studentPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询启用中的学生列表
     *
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询学生列表")
    public Result<List<Student>> list() {
        log.info("查询学生列表");
        List<Student> list = studentService.list();
        return Result.success(list);
    }

    /**
     * 新增学生（同时联动创建学生登录账号）
     *
     * @param studentDTO
     * @return
     */
    @PostMapping
    @Operation(summary = "新增学生")
    @Log(title = "学生档案管理")
    public Result save(@RequestBody StudentDTO studentDTO) {
        log.info("新增学生：{}", studentDTO);
        studentService.save(studentDTO);
        return Result.success();
    }

    /**
     * 修改学生
     *
     * @param studentDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改学生")
    @Log(title = "学生档案管理")
    public Result update(@RequestBody StudentDTO studentDTO) {
        log.info("修改学生：{}", studentDTO);
        studentService.update(studentDTO);
        return Result.success();
    }

    /**
     * 根据id查询学生
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询学生")
    public Result<Student> getById(@PathVariable Long id) {
        log.info("根据id查询学生：{}", id);
        Student student = studentService.getById(id);
        return Result.success(student);
    }

    /**
     * 根据关联账号id查询学生
     *
     * @param userId
     * @return
     */
    @GetMapping("/getByUserId/{userId}")
    @Operation(summary = "根据账号id查询学生")
    public Result<Student> getByUserId(@PathVariable Long userId) {
        log.info("根据账号id查询学生：{}", userId);
        Student student = studentService.getByUserId(userId);
        return Result.success(student);
    }

    /**
     * 删除学生
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除学生")
    @Log(title = "学生档案管理")
    public Result delete(@PathVariable Long id) {
        log.info("删除学生：{}", id);
        studentService.deleteById(id);
        return Result.success();
    }

}
