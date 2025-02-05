import {Component, Inject, ViewEncapsulation} from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'app-field-dialog',
  standalone: true,
  templateUrl: './field-dialog.component.html',
  styleUrls: ['./field-dialog.component.css'],
  encapsulation: ViewEncapsulation.None,  // ✅ Отключает инкапсуляцию стилей

  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule
  ]
})
export class FieldDialogComponent {
  fieldName: string = '';
  fieldType: string = 'text';
  fieldValue: string = '';
  isEditMode: boolean = false;

  constructor(
    private dialogRef: MatDialogRef<FieldDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    if (data && data.id) {
      this.isEditMode = true;
      this.fieldName = data.name || '';
      this.fieldType = data.fieldType || 'text';
      this.fieldValue = data.value || '';
    }
  }

  save(): void {
    this.dialogRef.close({
      name: this.fieldName,
      fieldType: this.fieldType,
      value: this.fieldValue
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
