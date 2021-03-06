package com.todo.controllers;

import com.todo.config.Pagination;
import com.todo.config.Route;
import com.todo.models.Project;
import com.todo.models.Task;
import com.todo.models.User;
import com.todo.services.ProjectService;
import com.todo.services.TaskService;
import com.todo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.Date;

/**
 * Created by masud on 2/17/17.
 */

@Controller
public class TasksController {

    private static final int MAX_PAGING_BUTTONS = 5;
    private static final int INITIAL_PAGE = 0;
    private static final int INITIAL_PAGE_SIZE = 10;
    private static final int[] PAGE_SIZES = { 5, 10, 20 };

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProjectService projectService;

    @RequestMapping(value = Route.TASKS_INDEX, method = RequestMethod.GET)
    public String index(Model model, @RequestParam(value = "pageSize", required = false) Integer pageSize,
                        @RequestParam(value = "page", required = false) Integer page) {

        int evalPageSize = pageSize == null ? INITIAL_PAGE_SIZE : pageSize;
        int evalPage = (page == null || page < 1) ? INITIAL_PAGE : page - 1;

        Page<Task> tasks = taskService.findAllPageable(new PageRequest(evalPage, evalPageSize));
        Pagination pagination = new Pagination(tasks.getTotalPages(), tasks.getNumber(), MAX_PAGING_BUTTONS);

        model.addAttribute("selectedPageSize", evalPageSize);
        model.addAttribute("pageSizes", PAGE_SIZES);
        model.addAttribute("pagination", pagination);
        model.addAttribute("route", Route.TASKS_INDEX);
        model.addAttribute("tasks", tasks);

        return "tasks/index";
    }

    @RequestMapping(value = Route.TASKS_NEW, method = RequestMethod.GET)
    public String newTask(Model model) {

        Iterable<Project> projects = projectService.findAll();
        Task task = new Task();
        task.setStatus("In progress");

        model.addAttribute("projects", projects);
        model.addAttribute(task);

        return "tasks/new";
    }

    @RequestMapping(value = Route.TASKS_CREATE, method = RequestMethod.POST)
    public String create(@Valid Task task, BindingResult bindingResult, Model model, @RequestParam long project_id) {

        if(bindingResult.hasErrors()) {
            return "/tasks/new";
        } else {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetails userDetail = (UserDetails) authentication.getPrincipal();
            User u = userService.findByEmail(userDetail.getUsername());
            task.setUser(u);
            task.setProject(projectService.findOne(project_id));
            task.setCreatedAt(new Date());
            task.setUpdatedAt(new Date());
            taskService.save(task);

            return "redirect:/tasks";
        }
    }

    @RequestMapping(value = Route.TASKS_SHOW, method = RequestMethod.GET)
    public String show(@PathVariable long id, Model model) {

        model.addAttribute("task", taskService.findOne(id));

        return "tasks/show";
    }

    @RequestMapping(value = Route.TASKS_EDIT, method = RequestMethod.GET)
    public String edit(@PathVariable long id, Model model) {

        Task task = taskService.findOne(id);
        model.addAttribute("task", task);

        return "tasks/edit";
    }

    @RequestMapping(value = Route.TASKS_UPDATE, method = RequestMethod.POST)
    public String update(@Valid Task task, BindingResult bindingResult, Model model) {

        if(bindingResult.hasErrors()) {
            return "tasks/edit";
        } else {

            Task updateTask = taskService.findOne(task.getId());
            updateTask.setSummary(task.getSummary());
            updateTask.setDescription(task.getDescription());
            updateTask.setStatus(task.getStatus());
            updateTask.setUpdatedAt(new Date());
            taskService.save(updateTask);

            return ("redirect:/tasks");
        }
    }

    @RequestMapping(value = Route.TASKS_DELETE, method = RequestMethod.GET)
    public ModelAndView delete(@PathVariable long id) {

        taskService.delete(id);

        return new ModelAndView("redirect:/tasks");
    }
}
