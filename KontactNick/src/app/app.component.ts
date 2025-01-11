import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true, // Указываем, что компонент standalone
  imports: [RouterOutlet], // Импорты для standalone компонента
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'] // styleUrls вместо styleUrl
})
export class AppComponent {
  title = 'KontactNick';
}
