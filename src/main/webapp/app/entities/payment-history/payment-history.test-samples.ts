import dayjs from 'dayjs/esm';

import { IPaymentHistory, NewPaymentHistory } from './payment-history.model';

export const sampleWithRequiredData: IPaymentHistory = {
  id: 53333,
};

export const sampleWithPartialData: IPaymentHistory = {
  id: 79322,
  organizationName: 'Steel',
  groupName: 'Lev Cloned invoice',
};

export const sampleWithFullData: IPaymentHistory = {
  id: 8897,
  organizationName: 'Legacy',
  serviceName: 'Small end-to-end Object-based',
  groupName: 'cyan B2C COM',
  sum: 61749,
  createdAt: dayjs('2022-11-13'),
};

export const sampleWithNewData: NewPaymentHistory = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
