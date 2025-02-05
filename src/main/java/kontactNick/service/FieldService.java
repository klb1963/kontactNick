package kontactNick.service;

import jakarta.persistence.EntityNotFoundException;
import kontactNick.dto.FieldDto;
import kontactNick.entity.Category;
import kontactNick.entity.Field;
import kontactNick.repository.CategoryRepository;
import kontactNick.repository.FieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FieldService {

    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // 📔add field to category
    public Field addFieldToCategory(Long categoryId, FieldDto fieldRequest, String userEmail) {
        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(userEmail)) // Проверка владельца
                .orElseThrow(() -> {
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied");
                });

        Field field = new Field();
        field.setName(fieldRequest.getName());
        field.setFieldType(fieldRequest.getFieldType());
        field.setValue(fieldRequest.getValue());
        field.setCategory(category);

        return fieldRepository.save(field); // Возвращаем сохранённое поле
    }

}
