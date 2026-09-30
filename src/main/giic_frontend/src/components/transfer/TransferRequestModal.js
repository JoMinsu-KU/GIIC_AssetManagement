import React, { useEffect, useState } from 'react';
import { getAssetLocations } from '../../api/assets';
import { createTransfer } from '../../api/transfers';
import '../../styles/Modal.css';

const TransferRequestModal = ({
    isOpen,
    onClose,
    assetId,
    assetName,
    currentLocation,
    currentUserName,
    currentUserDepartment,
    currentUserContact,
    onSuccess
}) => {
    const normalizedCurrentLocation = currentLocation?.trim() || '';
    const normalizedCurrentUserName = currentUserName?.trim() || '';
    const normalizedCurrentUserDepartment = currentUserDepartment?.trim() || '';
    const normalizedCurrentUserContact = currentUserContact?.trim() || '';
    const hasCurrentUser = Boolean(
        normalizedCurrentUserName
        || normalizedCurrentUserDepartment
        || normalizedCurrentUserContact
    );
    const [formData, setFormData] = useState({
        recipientMode: hasCurrentUser ? 'KEEP' : 'UNASSIGN',
        recipientName: '',
        recipientDepartment: '',
        recipientContact: '',
        recipientEmail: '',
        transferLocation: '',
        remarks: ''
    });
    const [locationMode, setLocationMode] = useState(normalizedCurrentLocation ? 'KEEP' : 'CHANGE');
    const [locationOptions, setLocationOptions] = useState([]);
    const [locationsLoading, setLocationsLoading] = useState(false);
    const [locationsError, setLocationsError] = useState('');
    const [locationRequestVersion, setLocationRequestVersion] = useState(0);
    const [isLocationDropdownOpen, setIsLocationDropdownOpen] = useState(false);
    const [highlightedLocationIndex, setHighlightedLocationIndex] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!isOpen) return;

        setLocationMode(normalizedCurrentLocation ? 'KEEP' : 'CHANGE');
        setFormData({
            recipientMode: hasCurrentUser ? 'KEEP' : 'UNASSIGN',
            recipientName: '',
            recipientDepartment: '',
            recipientContact: '',
            recipientEmail: '',
            transferLocation: '',
            remarks: ''
        });
        setIsLocationDropdownOpen(false);
        setHighlightedLocationIndex(0);
        setError('');
    }, [hasCurrentUser, isOpen, normalizedCurrentLocation]);

    useEffect(() => {
        if (!isOpen) return undefined;

        let active = true;
        setLocationsLoading(true);
        setLocationsError('');

        getAssetLocations()
            .then((locations) => {
                if (active) setLocationOptions(locations);
            })
            .catch((loadError) => {
                console.error('Failed to load asset locations:', loadError);
                if (active) {
                    setLocationOptions([]);
                    setLocationsError('비치장소 목록을 불러오지 못했습니다.');
                }
            })
            .finally(() => {
                if (active) setLocationsLoading(false);
            });

        return () => {
            active = false;
        };
    }, [isOpen, locationRequestVersion]);

    if (!isOpen) return null;

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleRecipientModeChange = (e) => {
        const recipientMode = e.target.value;
        setFormData((previous) => ({
            ...previous,
            recipientMode,
            ...(recipientMode !== 'CHANGE' ? {
                recipientName: '',
                recipientDepartment: '',
                recipientContact: '',
                recipientEmail: ''
            } : {})
        }));
        setError('');
    };

    const locationQuery = formData.transferLocation.trim().toLocaleLowerCase('ko-KR');
    const filteredLocations = locationQuery
        ? locationOptions
            .filter((location) => (
                location !== normalizedCurrentLocation
                && location.toLocaleLowerCase('ko-KR').includes(locationQuery)
            ))
            .slice(0, 10)
        : [];
    const registeredLocation = locationOptions.find(
        (location) => location === formData.transferLocation.trim()
    );

    const handleLocationModeChange = (e) => {
        setLocationMode(e.target.value);
        setIsLocationDropdownOpen(false);
        setHighlightedLocationIndex(0);
        setError('');
    };

    const handleLocationInputChange = (e) => {
        setFormData((previous) => ({ ...previous, transferLocation: e.target.value }));
        setIsLocationDropdownOpen(true);
        setHighlightedLocationIndex(0);
        setError('');
    };

    const selectLocation = (location) => {
        setFormData((previous) => ({ ...previous, transferLocation: location }));
        setIsLocationDropdownOpen(false);
        setHighlightedLocationIndex(0);
        setError('');
    };

    const handleLocationKeyDown = (e) => {
        if (e.key === 'Escape') {
            setIsLocationDropdownOpen(false);
            return;
        }

        if (!locationQuery || filteredLocations.length === 0) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setIsLocationDropdownOpen(true);
            setHighlightedLocationIndex((previous) => (
                previous >= filteredLocations.length - 1 ? 0 : previous + 1
            ));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setIsLocationDropdownOpen(true);
            setHighlightedLocationIndex((previous) => (
                previous <= 0 ? filteredLocations.length - 1 : previous - 1
            ));
        } else if (e.key === 'Enter' && isLocationDropdownOpen) {
            e.preventDefault();
            selectLocation(filteredLocations[highlightedLocationIndex] || filteredLocations[0]);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const requestedTransferLocation = formData.transferLocation.trim();
        const transferLocation = locationMode === 'KEEP'
            ? normalizedCurrentLocation
            : registeredLocation;
        const recipientRequired = formData.recipientMode === 'CHANGE';

        if (
            (recipientRequired && (
                !formData.recipientName.trim()
                || !formData.recipientDepartment.trim()
            ))
            || (locationMode === 'KEEP' ? !transferLocation : !requestedTransferLocation)
        ) {
            setError(recipientRequired
                ? '이름, 부서, 이관 장소는 필수 입력 항목입니다.'
                : '이관 장소는 필수 입력 항목입니다.');
            return;
        }

        if (locationMode === 'CHANGE' && requestedTransferLocation === normalizedCurrentLocation) {
            setError('장소를 변경하지 않으려면 현재 장소 유지를 선택해주세요.');
            return;
        }

        if (locationMode === 'CHANGE' && !registeredLocation) {
            setError('등록된 비치장소 목록에서 장소를 선택해주세요.');
            return;
        }

        if (formData.recipientMode === 'KEEP' && locationMode === 'KEEP') {
            setError('실사용자 또는 비치 장소 중 하나 이상을 변경해주세요.');
            return;
        }

        if (formData.recipientMode === 'UNASSIGN' && !hasCurrentUser && locationMode === 'KEEP') {
            setError('현재 실사용자가 없습니다. 비치 장소를 변경하거나 새 사용자를 지정해주세요.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            await createTransfer({
                assetId,
                ...formData,
                locationMode,
                transferLocation
            });
            alert('이관 신청이 완료되었습니다.');
            onSuccess && onSuccess();
            onClose();
        } catch (err) {
            console.error('Transfer request failed:', err);
            setError(err.response?.data?.message || '이관 신청에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="request-modal" onClick={(e) => e.stopPropagation()}>
                <div className="request-modal-header">
                    <h2>이관 신청</h2>
                    <button className="modal-close-btn" onClick={onClose}>&times;</button>
                </div>

                <div className="request-modal-body">
                    <p className="modal-subtitle">자산: <strong>{assetName}</strong></p>

                    {error && <div className="error-message modal-error">{error}</div>}

                    <form onSubmit={handleSubmit}>
                        <fieldset className="transfer-recipient-fieldset">
                            <legend>실사용자 처리</legend>
                            <div className="transfer-current-user">
                                <span>현재 실사용자</span>
                                <strong>
                                    {hasCurrentUser
                                        ? [normalizedCurrentUserName, normalizedCurrentUserDepartment]
                                            .filter(Boolean)
                                            .join(' · ')
                                        : '등록된 실사용자 없음'}
                                </strong>
                            </div>
                            <div className="transfer-recipient-options">
                                <label className={`transfer-recipient-option ${formData.recipientMode === 'KEEP' ? 'selected' : ''}`}>
                                    <input
                                        type="radio"
                                        name="recipientMode"
                                        value="KEEP"
                                        checked={formData.recipientMode === 'KEEP'}
                                        onChange={handleRecipientModeChange}
                                    />
                                    <span>
                                        <strong>현재 사용자 유지</strong>
                                        <small>실사용자 정보를 변경하지 않습니다.</small>
                                    </span>
                                </label>
                                <label className={`transfer-recipient-option ${formData.recipientMode === 'CHANGE' ? 'selected' : ''}`}>
                                    <input
                                        type="radio"
                                        name="recipientMode"
                                        value="CHANGE"
                                        checked={formData.recipientMode === 'CHANGE'}
                                        onChange={handleRecipientModeChange}
                                    />
                                    <span>
                                        <strong>다른 사용자로 변경</strong>
                                        <small>새 실사용자 정보를 입력합니다.</small>
                                    </span>
                                </label>
                                <label className={`transfer-recipient-option ${formData.recipientMode === 'UNASSIGN' ? 'selected' : ''}`}>
                                    <input
                                        type="radio"
                                        name="recipientMode"
                                        value="UNASSIGN"
                                        checked={formData.recipientMode === 'UNASSIGN'}
                                        onChange={handleRecipientModeChange}
                                    />
                                    <span>
                                        <strong>사용자 없음</strong>
                                        <small>승인 후 실사용자 정보를 비웁니다.</small>
                                    </span>
                                </label>
                            </div>
                        </fieldset>
                        {formData.recipientMode === 'CHANGE' && (
                            <div className="transfer-recipient-inputs">
                                <div className="form-group">
                                    <label>받는 사람 이름 <span className="required">*</span></label>
                                    <input
                                        type="text"
                                        name="recipientName"
                                        value={formData.recipientName}
                                        onChange={handleChange}
                                        placeholder="이름 입력"
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label>받는 부서 <span className="required">*</span></label>
                                    <input
                                        type="text"
                                        name="recipientDepartment"
                                        value={formData.recipientDepartment}
                                        onChange={handleChange}
                                        placeholder="부서명 입력"
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label>연락처</label>
                                    <input
                                        type="text"
                                        name="recipientContact"
                                        value={formData.recipientContact}
                                        onChange={handleChange}
                                        placeholder="010-0000-0000"
                                    />
                                </div>
                                <div className="form-group">
                                    <label>이메일</label>
                                    <input
                                        type="email"
                                        name="recipientEmail"
                                        value={formData.recipientEmail}
                                        onChange={handleChange}
                                        placeholder="example@company.com"
                                    />
                                </div>
                            </div>
                        )}
                        <fieldset className="transfer-location-fieldset">
                            <legend>이관 후 비치 장소 <span className="required">*</span></legend>
                            <div className="transfer-current-location">
                                <span>현재 비치 장소</span>
                                <strong>{normalizedCurrentLocation || '등록된 장소 없음'}</strong>
                            </div>
                            <div className="transfer-location-options">
                                <label className={`transfer-location-option ${locationMode === 'KEEP' ? 'selected' : ''} ${!normalizedCurrentLocation ? 'disabled' : ''}`}>
                                    <input
                                        type="radio"
                                        name="locationMode"
                                        value="KEEP"
                                        checked={locationMode === 'KEEP'}
                                        onChange={handleLocationModeChange}
                                        disabled={!normalizedCurrentLocation}
                                    />
                                    <span>
                                        <strong>현재 장소 유지</strong>
                                        <small>사용자만 변경되고 자산은 이동하지 않습니다.</small>
                                    </span>
                                </label>
                                <label className={`transfer-location-option ${locationMode === 'CHANGE' ? 'selected' : ''}`}>
                                    <input
                                        type="radio"
                                        name="locationMode"
                                        value="CHANGE"
                                        checked={locationMode === 'CHANGE'}
                                        onChange={handleLocationModeChange}
                                    />
                                    <span>
                                        <strong>다른 장소로 변경</strong>
                                        <small>이관 후 실제 비치 장소를 새로 입력합니다.</small>
                                    </span>
                                </label>
                            </div>
                            {locationMode === 'CHANGE' && (
                                <div className="transfer-new-location">
                                    <label htmlFor="transferLocation">새 비치 장소</label>
                                    <div className="transfer-location-combobox">
                                        <input
                                            id="transferLocation"
                                            type="text"
                                            name="transferLocation"
                                            value={formData.transferLocation}
                                            onChange={handleLocationInputChange}
                                            onFocus={() => setIsLocationDropdownOpen(Boolean(locationQuery))}
                                            onBlur={() => setIsLocationDropdownOpen(false)}
                                            onKeyDown={handleLocationKeyDown}
                                            placeholder="한 글자 이상 입력해 장소 검색"
                                            role="combobox"
                                            aria-autocomplete="list"
                                            aria-controls="transfer-location-listbox"
                                            aria-expanded={isLocationDropdownOpen && Boolean(locationQuery)}
                                            aria-activedescendant={
                                                isLocationDropdownOpen && filteredLocations.length > 0
                                                    ? `transfer-location-option-${highlightedLocationIndex}`
                                                    : undefined
                                            }
                                            autoComplete="off"
                                            required
                                        />
                                        {isLocationDropdownOpen && locationQuery && !locationsLoading && !locationsError && (
                                            <ul
                                                id="transfer-location-listbox"
                                                className="transfer-location-dropdown"
                                                role="listbox"
                                            >
                                                {filteredLocations.length > 0 ? filteredLocations.map((location, index) => (
                                                    <li key={location} role="presentation">
                                                        <button
                                                            id={`transfer-location-option-${index}`}
                                                            className={index === highlightedLocationIndex ? 'highlighted' : ''}
                                                            type="button"
                                                            role="option"
                                                            aria-selected={location === registeredLocation}
                                                            onMouseDown={(event) => {
                                                                event.preventDefault();
                                                                selectLocation(location);
                                                            }}
                                                            onMouseEnter={() => setHighlightedLocationIndex(index)}
                                                        >
                                                            {location}
                                                        </button>
                                                    </li>
                                                )) : (
                                                    <li className="transfer-location-empty">일치하는 등록 장소가 없습니다.</li>
                                                )}
                                            </ul>
                                        )}
                                    </div>
                                    {locationsLoading && <p className="transfer-location-help">비치장소 목록을 불러오는 중입니다.</p>}
                                    {locationsError && (
                                        <p className="transfer-location-help is-error">
                                            {locationsError}
                                            <button type="button" onClick={() => setLocationRequestVersion((version) => version + 1)}>
                                                다시 불러오기
                                            </button>
                                        </p>
                                    )}
                                    {!locationsLoading && !locationsError && registeredLocation && (
                                        <p className="transfer-location-help is-selected">등록된 비치장소가 선택되었습니다.</p>
                                    )}
                                </div>
                            )}
                        </fieldset>
                        <div className="form-group">
                            <label>비고</label>
                            <textarea
                                name="remarks"
                                value={formData.remarks}
                                onChange={handleChange}
                                placeholder="추가 요청사항이나 메모"
                                rows="3"
                            />
                        </div>

                        <div className="request-modal-footer">
                            <button type="button" className="btn-cancel" onClick={onClose} disabled={loading}>
                                취소
                            </button>
                            <button type="submit" className="btn-submit" disabled={loading}>
                                {loading ? '처리중...' : '신청하기'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default TransferRequestModal;
