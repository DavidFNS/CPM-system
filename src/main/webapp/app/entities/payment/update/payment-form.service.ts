import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IPayment, NewPayment } from '../payment.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IPayment for edit and NewPaymentFormGroupInput for create.
 */
type PaymentFormGroupInput = IPayment | PartialWithRequiredKeyOf<NewPayment>;

type PaymentFormDefaults = Pick<NewPayment, 'id' | 'isPayed'>;

type PaymentFormGroupContent = {
  id: FormControl<IPayment['id'] | NewPayment['id']>;
  payedMoney: FormControl<IPayment['payedMoney']>;
  paymentForPeriod: FormControl<IPayment['paymentForPeriod']>;
  isPayed: FormControl<IPayment['isPayed']>;
  startedPeriod: FormControl<IPayment['startedPeriod']>;
  finishedPeriod: FormControl<IPayment['finishedPeriod']>;
  customer: FormControl<IPayment['customer']>;
  service: FormControl<IPayment['service']>;
  group: FormControl<IPayment['group']>;
};

export type PaymentFormGroup = FormGroup<PaymentFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class PaymentFormService {
  createPaymentFormGroup(payment: PaymentFormGroupInput = { id: null }): PaymentFormGroup {
    const paymentRawValue = {
      ...this.getFormDefaults(),
      ...payment,
    };
    return new FormGroup<PaymentFormGroupContent>({
      id: new FormControl(
        { value: paymentRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),
      payedMoney: new FormControl(paymentRawValue.payedMoney, {
        validators: [Validators.required],
      }),
      paymentForPeriod: new FormControl(paymentRawValue.paymentForPeriod, {
        validators: [Validators.required],
      }),
      isPayed: new FormControl(paymentRawValue.isPayed, {
        validators: [Validators.required],
      }),
      startedPeriod: new FormControl(paymentRawValue.startedPeriod, {
        validators: [Validators.required],
      }),
      finishedPeriod: new FormControl(paymentRawValue.finishedPeriod, {
        validators: [Validators.required],
      }),
      customer: new FormControl(paymentRawValue.customer),
      service: new FormControl(paymentRawValue.service),
      group: new FormControl(paymentRawValue.group),
    });
  }

  getPayment(form: PaymentFormGroup): IPayment | NewPayment {
    return form.getRawValue() as IPayment | NewPayment;
  }

  resetForm(form: PaymentFormGroup, payment: PaymentFormGroupInput): void {
    const paymentRawValue = { ...this.getFormDefaults(), ...payment };
    form.reset(
      {
        ...paymentRawValue,
        id: { value: paymentRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */
    );
  }

  private getFormDefaults(): PaymentFormDefaults {
    return {
      id: null,
      isPayed: false,
    };
  }
}
