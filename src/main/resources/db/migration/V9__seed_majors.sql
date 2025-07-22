INSERT INTO majors (
    major_name, major_code, major_code_old,
    uk_lang_coef, math_coef, history_coef, uk_literature_coef,
    foreign_lang_coef, biology_coef, geography_coef,
    physics_coef, chemistry_coef,
    competition_coef, major_coef
) VALUES
      (
          'Інженерія програмного забезпечення', 'f2', 121,
          0.3, 0.5, 0.2, 0.2,
          0.3, 0.2, 0.2,
          0.4, 0.3,
          0, 1
      ),
      (
          'Комп''ютерні науки', 'f3', 122,
          0.3, 0.5, 0.2, 0.2,
          0.3, 0.2, 0.2,
          0.4, 0.3,
          0, 1
      ),
      (
          'Прикладна математика', 'f1', 113,
          0.3, 0.5, 0.2, 0.2,
          0.3, 0.2, 0.2,
          0.4, 0.3,
          0, 1
      );