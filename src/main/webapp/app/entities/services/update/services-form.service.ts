import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IServices, NewServices } from '../services.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IServices for edit and NewServicesFormGroupInput for create.
 */
type ServicesFormGroupInput = IServices | PartialWithRequiredKeyOf<NewServices>;

type ServicesFormDefaults = Pick<NewServices, 'id'>;

type ServicesFormGroupContent = {
  id: FormControl<IServices['id'] | NewServices['id']>;
  name: FormControl<IServices['name']>;
  price: FormControl<IServices['price']>;
  startedPeriod: FormControl<IServices['startedPeriod']>;
  periodType: FormControl<IServices['periodType']>;
  countPeriod: FormControl<IServices['countPeriod']>;
  group: FormControl<IServices['group']>;
};

export type ServicesFormGroup = FormGroup<ServicesFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ServicesFormService {
  createServicesFormGroup(services: ServicesFormGroupInput = { id: null }): ServicesFormGroup {
    const servicesRawValue = {
      ...this.getFormDefaults(),
      ...services,
    };
    return new FormGroup<ServicesFormGroupContent>({
      id: new FormControl(
        { value: servicesRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),
      name: new FormControl(servicesRawValue.name, {
        validators: [Validators.required],
      }),
      price: new FormControl(servicesRawValue.price, {
        validators: [Validators.required, Validators.min(10000)],
      }),
      startedPeriod: new FormControl(servicesRawValue.startedPeriod, {
        validators: [Validators.required],
      }),
      periodType: new FormControl(servicesRawValue.periodType, {
        validators: [Validators.required],
      }),
      countPeriod: new FormControl(servicesRawValue.countPeriod, {
        validators: [Validators.required, Validators.min(1)],
      }),
      group: new FormControl(servicesRawValue.group),
    });
  }

  getServices(form: ServicesFormGroup): IServices | NewServices {
    return form.getRawValue() as IServices | NewServices;
  }

  resetForm(form: ServicesFormGroup, services: ServicesFormGroupInput): void {
    const servicesRawValue = { ...this.getFormDefaults(), ...services };
    form.reset(
      {
        ...servicesRawValue,
        id: { value: servicesRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */
    );
  }

  private getFormDefaults(): ServicesFormDefaults {
    return {
      id: null,
    };
  }
}
