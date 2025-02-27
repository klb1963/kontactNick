package kontactNick.service;

import jakarta.persistence.EntityNotFoundException;
import kontactNick.dto.FieldDto;
import kontactNick.entity.Category;
import kontactNick.entity.Field;
import kontactNick.repository.CategoryRepository;
import kontactNick.repository.FieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldService {

    private final FieldRepository fieldRepository;
    private final CategoryRepository categoryRepository;

    // 📔 Получение всех полей категории
    public List<FieldDto> getFieldsByCategory(Long categoryId, String userEmail) {
        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(userEmail)) // Проверка владельца
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied"));

        return category.getFields().stream()
                .map(field -> new FieldDto(field.getId(), field.getName(), field.getDescription(), field.getFieldType(), field.getValue()))
                .collect(Collectors.toList());
    }

    // 📔 Добавление поля в категорию
    public Field addFieldToCategory(Long categoryId, FieldDto fieldRequest, String userEmail) {
        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(userEmail)) // Проверка владельца
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied"));

        Field field = new Field();
        field.setName(fieldRequest.getName());
        field.setFieldType(fieldRequest.getFieldType());
        field.setValue(fieldRequest.getValue());
        field.setCategory(category);

        return fieldRepository.save(field);
    }

    // 🔄 Обновление поля в категории
    public Field updateField(Long categoryId, Long fieldId, FieldDto updatedField, String userEmail) {
        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(userEmail)) // Проверка владельца
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied"));

        Field field = fieldRepository.findById(fieldId)
                .filter(f -> f.getCategory().getId().equals(categoryId)) // Проверка принадлежности поля категории
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found"));

        field.setName(updatedField.getName());
        field.setFieldType(updatedField.getFieldType());
        field.setValue(updatedField.getValue());

        return fieldRepository.save(field);
    }

    // 🗑 Удаление поля из категории
    public void deleteField(Long categoryId, Long fieldId, String userEmail) {
        Category category = categoryRepository.findById(categoryId)
                .filter(cat -> cat.getUser().getEmail().equals(userEmail)) // Проверка владельца
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or access denied"));

        Field field = fieldRepository.findById(fieldId)
                .filter(f -> f.getCategory().getId().equals(categoryId)) // Проверка принадлежности
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Field not found or doesn't belong to category"));

        fieldRepository.delete(field);
    }
}
